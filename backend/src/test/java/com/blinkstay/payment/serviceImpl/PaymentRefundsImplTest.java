package com.blinkstay.payment.serviceImpl;

import com.blinkstay.payment.entities.Payment;
import com.blinkstay.payment.enums.PaymentStatus;
import com.blinkstay.payment.event.RefundRequestedEvent;
import com.blinkstay.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.blinkstay.support.builders.PaymentBuilder.aPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Booking asks payment for a refund (Q65): it is only RECORDED here (REFUND_PENDING);
 * RefundProcessor sends it after the commit. Asking twice is safe.
 */
@ExtendWith(MockitoExtension.class)
class PaymentRefundsImplTest {

    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private ApplicationEventPublisher publisher;
    @InjectMocks
    private PaymentRefundsImpl refunds;

    private final Payment payment = aPayment().status(PaymentStatus.SUCCEEDED).amount("4000.00").build();

    @Test
    void succeededPayment_isMarkedRefundPending_andAnnounced() {
        when(paymentRepo.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        BigDecimal amount = refunds.refundInFull(payment.getId(), "Booking T1 was cancelled");

        assertThat(amount).isEqualByComparingTo("4000.00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(payment.getRefundReason()).isEqualTo("Booking T1 was cancelled");
        assertThat(payment.getRefundNotify()).isFalse(); // the cancellation email already says it
        assertThat(payment.getRefundAttempts()).isZero();
        assertThat(payment.getNextRefundAt()).isNotNull();
        verify(paymentRepo).save(payment);

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(published.capture());
        assertThat(published.getValue()).isEqualTo(new RefundRequestedEvent(payment.getId()));
    }

    @ParameterizedTest(name = "already {0} -> same amount, nothing new")
    @EnumSource(value = PaymentStatus.class, names = {"REFUND_PENDING", "REFUNDED", "REFUND_FAILED"})
    void refundAlreadyRequested_isIdempotent(PaymentStatus status) {
        payment.setStatus(status);
        when(paymentRepo.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        BigDecimal amount = refunds.refundInFull(payment.getId(), "again");

        assertThat(amount).isEqualByComparingTo("4000.00");
        assertThat(payment.getStatus()).isEqualTo(status);
        verify(paymentRepo, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @ParameterizedTest(name = "{0} -> 409")
    @EnumSource(value = PaymentStatus.class, names = {"CREATED", "FAILED"})
    void paymentThatNeverSucceeded_cannotBeRefunded(PaymentStatus status) {
        payment.setStatus(status);
        when(paymentRepo.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> refunds.refundInFull(payment.getId(), "x"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void veryLongReason_isCutTo255() {
        when(paymentRepo.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        refunds.refundInFull(payment.getId(), "r".repeat(300));

        assertThat(payment.getRefundReason()).hasSize(255);
    }

    @Test
    void unknownPayment_isNotFound_forRefundAndForLock() {
        UUID unknown = UUID.randomUUID();
        when(paymentRepo.findByIdForUpdate(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refunds.refundInFull(unknown, "x"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> refunds.lockPayment(unknown))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}