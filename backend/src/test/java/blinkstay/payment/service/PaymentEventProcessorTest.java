package blinkstay.payment.service;

import blinkstay.common.booking.BookingPayments;
import blinkstay.common.booking.PaymentConfirmation;
import blinkstay.payment.entities.Payment;
import blinkstay.payment.entities.PaymentEvent;
import blinkstay.payment.enums.PaymentStatus;
import blinkstay.payment.enums.ProviderEventType;
import blinkstay.payment.event.RefundRequestedEvent;
import blinkstay.payment.gateway.PaymentGateway;
import blinkstay.payment.gateway.ProviderEvent;
import blinkstay.payment.repository.PaymentEventRepository;
import blinkstay.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static blinkstay.support.builders.PaymentBuilder.aPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The one processor behind verify, webhook and reconcile (Q57, Q58, Q73, Q74).
 * Rules: unknown orders are ignored; an event id is processed once; final payment states never
 * change; success asks booking to confirm and reacts to the 5 possible answers.
 * <p>
 * Unit test with Mockito: repositories, booking module and event publisher are fakes.
 * (The row lock itself is a database feature; it gets a real-Postgres test in T4.)
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventProcessorTest {

    private static final String RAZORPAY = "RAZORPAY";

    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private PaymentEventRepository eventRepo;
    @Mock
    private BookingPayments bookings;
    @Mock
    private PaymentGateway gateway;
    @Mock
    private ApplicationEventPublisher publisher;
    @InjectMocks
    private PaymentEventProcessor processor;

    private final Payment payment = aPayment().orderId("order_1").amount("4000.00").build();

    // ---------------- idempotency ----------------

    @Nested
    @DisplayName("idempotency: the same payment reported more than once")
    class Idempotency {

        @Test
        void sameEventDeliveredTwice_confirmsTheBookingOnlyOnce() {
            // Arrange: first delivery is new, the second is a provider retry of the same event id
            ProviderEvent event = succeeded("evt_1");
            when(paymentRepo.findByProviderOrderIdForUpdate("order_1")).thenReturn(Optional.of(payment));
            when(eventRepo.existsByProviderAndProviderEventId(RAZORPAY, "evt_1")).thenReturn(false, true);
            when(bookings.confirmPayment(payment.getBookingId(), payment.getId(), event.amount(), "INR"))
                    .thenReturn(PaymentConfirmation.CONFIRMED);

            // Act
            processor.process(RAZORPAY, event);
            processor.process(RAZORPAY, event);

            // Assert
            verify(bookings, times(1)).confirmPayment(any(), any(), any(), any());
            verify(eventRepo, times(1)).save(any());
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        }

        @Test
        void verifyThenWebhook_differentEventIds_secondIsRecordedButChangesNothing() {
            // browser verify ("verify_pay_1") and Razorpay's webhook ("evt_1") report the same payment
            when(paymentRepo.findByProviderOrderIdForUpdate("order_1")).thenReturn(Optional.of(payment));
            when(eventRepo.existsByProviderAndProviderEventId(any(), any())).thenReturn(false);
            when(bookings.confirmPayment(any(), any(), any(), any())).thenReturn(PaymentConfirmation.CONFIRMED);

            processor.process(RAZORPAY, succeeded("verify_pay_1"));
            processor.process(RAZORPAY, succeeded("evt_1"));

            verify(bookings, times(1)).confirmPayment(any(), any(), any(), any());
            verify(eventRepo, times(2)).save(any()); // both event ids are remembered
        }

        @ParameterizedTest(name = "payment already {0} -> nothing changes")
        @EnumSource(value = PaymentStatus.class, names = {"SUCCEEDED", "REFUNDED", "REFUND_PENDING", "REFUND_FAILED"})
        void finalStates_neverChangeAgain(PaymentStatus finalStatus) {
            payment.setStatus(finalStatus);
            when(paymentRepo.findByProviderOrderIdForUpdate("order_1")).thenReturn(Optional.of(payment));
            when(eventRepo.existsByProviderAndProviderEventId(RAZORPAY, "evt_late")).thenReturn(false);

            processor.process(RAZORPAY, failed("evt_late", "late failure"));

            assertThat(payment.getStatus()).isEqualTo(finalStatus);
            verifyNoInteractions(bookings);
            verify(paymentRepo, never()).save(any());
        }
    }

    // ---------------- events we must ignore ----------------

    @Nested
    @DisplayName("ignored events")
    class Ignored {

        @Test
        void unknownOrder_isIgnored() {
            when(paymentRepo.findByProviderOrderIdForUpdate("order_1")).thenReturn(Optional.empty());

            processor.process(RAZORPAY, succeeded("evt_1"));

            verifyNoInteractions(eventRepo, bookings, publisher);
        }

        @Test
        void orderOfAnotherProvider_isTreatedAsUnknown() {
            // a mock-gateway payment must never be confirmed by a Razorpay event
            Payment mockPayment = aPayment().provider("MOCK").orderId("order_1").build();
            when(paymentRepo.findByProviderOrderIdForUpdate("order_1")).thenReturn(Optional.of(mockPayment));

            processor.process(RAZORPAY, succeeded("evt_1"));

            verifyNoInteractions(eventRepo, bookings, publisher);
            assertThat(mockPayment.getStatus()).isEqualTo(PaymentStatus.CREATED);
        }
    }

    // ---------------- payment failed ----------------

    @Nested
    @DisplayName("PAYMENT_FAILED")
    class Failed {

        @Test
        void failedEvent_marksPaymentFailedWithReason_andRecordsTheEvent() {
            arrangeNewEvent("evt_f1");

            processor.process(RAZORPAY, failed("evt_f1", "Card declined"));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo("Card declined");
            verify(paymentRepo).save(payment);

            ArgumentCaptor<PaymentEvent> stored = ArgumentCaptor.forClass(PaymentEvent.class);
            verify(eventRepo).save(stored.capture());
            assertThat(stored.getValue().getProviderEventId()).isEqualTo("evt_f1");
            assertThat(stored.getValue().getType()).isEqualTo("PAYMENT_FAILED");
            assertThat(stored.getValue().getOrderId()).isEqualTo("order_1");
            verifyNoInteractions(bookings);
        }

        @Test
        void failedWithoutReason_usesDefaultText() {
            arrangeNewEvent("evt_f2");

            processor.process(RAZORPAY, failed("evt_f2", null));

            assertThat(payment.getFailureReason()).isEqualTo("Payment failed");
        }

        @Test
        void veryLongReason_isCutTo255Characters() {
            arrangeNewEvent("evt_f3");

            processor.process(RAZORPAY, failed("evt_f3", "x".repeat(400)));

            assertThat(payment.getFailureReason()).hasSize(255);
        }
    }

    // ---------------- payment succeeded: the 5 answers from booking ----------------

    @Nested
    @DisplayName("PAYMENT_SUCCEEDED")
    class Succeeded {

        @ParameterizedTest(name = "booking answers {0} -> payment SUCCEEDED, no refund")
        @EnumSource(value = PaymentConfirmation.class, names = {"CONFIRMED", "ALREADY_CONFIRMED"})
        void bookingConfirmed_marksPaid(PaymentConfirmation answer) {
            arrangeSuccess("evt_s1", answer);

            processor.process(RAZORPAY, succeeded("evt_s1"));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(payment.getProviderPaymentId()).isEqualTo("pay_123");
            assertThat(payment.getPaidAt()).isNotNull();
            verifyNoInteractions(publisher);
        }

        @Test
        void secondPaymentForSameBooking_isKeptThenRefunded() {
            arrangeSuccess("evt_s2", PaymentConfirmation.DUPLICATE);

            processor.process(RAZORPAY, succeeded("evt_s2"));

            assertRefundRequested("paid twice");
        }

        @Test
        void holdExpiredAndRoomsGone_isRefunded() {
            arrangeSuccess("evt_s3", PaymentConfirmation.UNAVAILABLE);

            processor.process(RAZORPAY, succeeded("evt_s3"));

            assertRefundRequested("could not be kept");
        }

        @Test
        void amountDoesNotMatchBooking_isNeverConfirmed_andFlaggedForReview() {
            arrangeSuccess("evt_s4", PaymentConfirmation.MISMATCH);

            processor.process(RAZORPAY, succeeded("evt_s4"));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getProviderPaymentId()).isEqualTo("pay_123"); // kept, so the money can be found
            assertThat(payment.getReviewReason()).contains("4000.00");
            assertThat(payment.getPaidAt()).isNull();
            verifyNoInteractions(publisher); // no automatic refund either: a person decides
        }

        @Test
        void success_passesTheProvidersAmountAndCurrencyToBooking() {
            arrangeSuccess("evt_s5", PaymentConfirmation.CONFIRMED);

            processor.process(RAZORPAY, succeeded("evt_s5"));

            verify(bookings).confirmPayment(payment.getBookingId(), payment.getId(),
                    new BigDecimal("4000.00"), "INR");
        }

        private void assertRefundRequested(String reasonPart) {
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
            assertThat(payment.getProviderPaymentId()).isEqualTo("pay_123");
            assertThat(payment.getPaidAt()).isNotNull();
            assertThat(payment.getRefundReason()).contains(reasonPart);
            assertThat(payment.getRefundNotify()).isTrue();
            assertThat(payment.getRefundAttempts()).isZero();
            assertThat(payment.getNextRefundAt()).isNotNull();

            // the refund is only RECORDED here; RefundProcessor sends it after the commit (Q65)
            ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
            verify(publisher).publishEvent(published.capture());
            assertThat(published.getValue()).isInstanceOf(RefundRequestedEvent.class)
                    .usingRecursiveComparison().isEqualTo(new RefundRequestedEvent(payment.getId()));
        }
    }

    // ---------------- helpers ----------------

    private void arrangeNewEvent(String eventId) {
        when(paymentRepo.findByProviderOrderIdForUpdate("order_1")).thenReturn(Optional.of(payment));
        when(eventRepo.existsByProviderAndProviderEventId(RAZORPAY, eventId)).thenReturn(false);
    }

    private void arrangeSuccess(String eventId, PaymentConfirmation answer) {
        arrangeNewEvent(eventId);
        when(bookings.confirmPayment(any(), any(), any(), any())).thenReturn(answer);
    }

    private static ProviderEvent succeeded(String eventId) {
        return new ProviderEvent(eventId, ProviderEventType.PAYMENT_SUCCEEDED, "order_1", "pay_123",
                new BigDecimal("4000.00"), "INR", null);
    }

    private static ProviderEvent failed(String eventId, String reason) {
        return new ProviderEvent(eventId, ProviderEventType.PAYMENT_FAILED, "order_1", null, null, null, reason);
    }
}