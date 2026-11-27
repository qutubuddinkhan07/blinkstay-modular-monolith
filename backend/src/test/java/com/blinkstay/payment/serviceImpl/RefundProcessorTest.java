package com.blinkstay.payment.serviceImpl;

import com.blinkstay.common.booking.PaymentRefundedEvent;
import com.blinkstay.payment.entities.Payment;
import com.blinkstay.payment.enums.PaymentStatus;
import com.blinkstay.payment.gateway.PaymentGateway;
import com.blinkstay.payment.gateway.ProviderRefund;
import com.blinkstay.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.blinkstay.support.builders.PaymentBuilder.aPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * The refund worker (Q65, Q66): claim the row with a 5-minute lease, call the provider OUTSIDE a
 * transaction, then record success or failure. Retries back off 1, 5, 15, 60 minutes; after 5
 * failed attempts the payment becomes REFUND_FAILED for an admin. A wrong active provider is never called.
 */
@ExtendWith(MockitoExtension.class)
class RefundProcessorTest {

    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private PaymentGateway gateway;
    @Mock
    private ApplicationEventPublisher publisher;

    private RefundProcessor processor;
    private Payment payment;

    /**
     * A FAKE (not a mock): a TransactionTemplate that simply runs the code it is given.
     * There is no database here, and we want the real claim / record logic inside it to run.
     */
    private static final TransactionTemplate RUN_DIRECTLY = new TransactionTemplate() {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(null);
        }
    };

    @BeforeEach
    void setUp() {
        // built by hand (not @InjectMocks) because one dependency is a fake, not a mock
        processor = new RefundProcessor(paymentRepo, gateway, RUN_DIRECTLY, publisher);
    }

    /**
     * A payment waiting for its refund, due now, made with RAZORPAY.
     */
    private Payment pending(int attemptsSoFar) {
        Payment p = aPayment().status(PaymentStatus.REFUND_PENDING).amount("4000.00").build();
        p.setProviderPaymentId("pay_123");
        p.setRefundReason("Booking T1 was cancelled");
        p.setRefundAttempts(attemptsSoFar);
        p.setNextRefundAt(Instant.now().minusSeconds(1));
        when(paymentRepo.findByIdForUpdate(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    @Nested
    @DisplayName("success")
    class Success {

        @Test
        void dueRefund_isSentToProvider_andMarkedRefunded() {
            payment = pending(0);
            when(gateway.name()).thenReturn("RAZORPAY");
            when(gateway.refund("pay_123", payment.getAmount(), "Booking T1 was cancelled"))
                    .thenReturn(new ProviderRefund("rfnd_1"));

            processor.attempt(payment.getId());

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.getRefundedAmount()).isEqualByComparingTo("4000.00");
            assertThat(payment.getRefundedAt()).isNotNull();
            assertThat(payment.getNextRefundAt()).isNull();
            assertThat(payment.getRefundError()).isNull();
            assertThat(payment.getRefundAttempts()).isEqualTo(1);
        }

        @Test
        void guestIsNotified_onlyWhenRefundNotifyIsTrue() {
            payment = pending(0);
            payment.setRefundNotify(true); // e.g. a duplicate payment refunded by the processor
            when(gateway.name()).thenReturn("RAZORPAY");
            when(gateway.refund(any(), any(), any())).thenReturn(new ProviderRefund("rfnd_1"));

            processor.attempt(payment.getId());

            ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
            verify(publisher).publishEvent(published.capture());
            assertThat(published.getValue()).isEqualTo(new PaymentRefundedEvent(payment.getId(),
                    payment.getBookingId(), payment.getGuestId(), payment.getBookingReference(),
                    payment.getAmount(), payment.getCurrency(), payment.getRefundReason()));
        }

        @Test
        void cancellationRefund_sendsNoExtraEmail() {
            payment = pending(0);
            payment.setRefundNotify(false); // the cancellation email already told the guest
            when(gateway.name()).thenReturn("RAZORPAY");
            when(gateway.refund(any(), any(), any())).thenReturn(new ProviderRefund("rfnd_1"));

            processor.attempt(payment.getId());

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verifyNoInteractions(publisher);
        }
    }

    @Nested
    @DisplayName("nothing to do")
    class NothingToDo {

        @Test
        void notDueYet_leaseHeldByAnotherWorker_providerNotCalled() {
            payment = pending(1);
            payment.setNextRefundAt(Instant.now().plus(Duration.ofMinutes(4)));

            processor.attempt(payment.getId());

            verify(gateway, never()).refund(any(), any(), any());
            assertThat(payment.getRefundAttempts()).isEqualTo(1); // not counted
        }

        @Test
        void alreadyRefunded_providerNotCalled() {
            payment = pending(1);
            payment.setStatus(PaymentStatus.REFUNDED);

            processor.attempt(payment.getId());

            verifyNoInteractions(gateway);
        }

        @Test
        void unknownPayment_providerNotCalled() {
            UUID unknown = UUID.randomUUID();
            when(paymentRepo.findByIdForUpdate(unknown)).thenReturn(Optional.empty());

            processor.attempt(unknown);

            verifyNoInteractions(gateway);
        }
    }

    @Nested
    @DisplayName("failure and retries")
    class Failure {

        @ParameterizedTest(name = "attempt {0} fails -> retry in {1} min")
        @CsvSource({"1, 1", "2, 5", "3, 15", "4, 60"})
        void failedAttempt_staysPending_withBackoff(int attemptNumber, long minutes) {
            payment = pending(attemptNumber - 1);
            when(gateway.name()).thenReturn("RAZORPAY");
            when(gateway.refund(any(), any(), any())).thenThrow(new RuntimeException("invalid request sent"));

            Instant before = Instant.now();
            processor.attempt(payment.getId());

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
            assertThat(payment.getRefundAttempts()).isEqualTo(attemptNumber);
            assertThat(payment.getRefundError()).isEqualTo("invalid request sent");
            assertThat(payment.getNextRefundAt())
                    .isBetween(before.plus(Duration.ofMinutes(minutes)),
                            Instant.now().plus(Duration.ofMinutes(minutes)));
        }

        @Test
        void fifthFailure_givesUp_andNeedsAnAdmin() {
            payment = pending(4);
            when(gateway.name()).thenReturn("RAZORPAY");
            when(gateway.refund(any(), any(), any())).thenThrow(new RuntimeException("still failing"));

            processor.attempt(payment.getId());

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_FAILED);
            assertThat(payment.getRefundAttempts()).isEqualTo(5);
            assertThat(payment.getNextRefundAt()).isNull();
        }

        @Test
        void paymentFromAnotherProvider_isNeverSentToTheActiveOne() {
            // made with the mock gateway, but Razorpay is active now (Q60)
            payment = pending(0);
            payment.setProvider("MOCK");
            when(gateway.name()).thenReturn("RAZORPAY");

            processor.attempt(payment.getId());

            verify(gateway, never()).refund(any(), any(), any());
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
            assertThat(payment.getRefundError()).contains("MOCK").contains("RAZORPAY");
        }

        @Test
        void errorWithoutMessage_storesTheExceptionName() {
            payment = pending(0);
            when(gateway.name()).thenReturn("RAZORPAY");
            when(gateway.refund(any(), any(), any())).thenThrow(new IllegalStateException());

            processor.attempt(payment.getId());

            assertThat(payment.getRefundError()).isEqualTo("IllegalStateException");
        }

        @Test
        void veryLongError_isCutTo255() {
            payment = pending(0);
            when(gateway.name()).thenReturn("RAZORPAY");
            when(gateway.refund(any(), any(), any())).thenThrow(new RuntimeException("e".repeat(400)));

            processor.attempt(payment.getId());

            assertThat(payment.getRefundError()).hasSize(255);
        }

        @Test
        void attempt_neverThrows_evenWhenTheDatabaseFails() {
            UUID id = UUID.randomUUID();
            when(paymentRepo.findByIdForUpdate(id)).thenThrow(new RuntimeException("connection lost"));

            assertThatCode(() -> processor.attempt(id)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("runDue (the 30-second timer)")
    class RunDue {

        @Test
        void triesEveryDueRefund_andReturnsHowMany() {
            Payment a = pending(0);
            Payment b = pending(0);
            when(paymentRepo.findDueRefundIds(any(Instant.class), eq(PageRequest.of(0, 20))))
                    .thenReturn(List.of(a.getId(), b.getId()));
            when(gateway.name()).thenReturn("RAZORPAY");
            when(gateway.refund(any(), any(), any())).thenReturn(new ProviderRefund("rfnd"));

            int tried = processor.runDue();

            assertThat(tried).isEqualTo(2);
            assertThat(a.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(b.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }
    }
}