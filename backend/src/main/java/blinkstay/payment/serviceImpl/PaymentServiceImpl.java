package blinkstay.payment.serviceImpl;

import blinkstay.common.booking.BookingPayments;
import blinkstay.common.booking.PayableBooking;
import blinkstay.payment.dto.PaymentResultDto;
import blinkstay.payment.dto.PaymentSessionDto;
import blinkstay.payment.entities.Payment;
import blinkstay.payment.enums.MockOutcome;
import blinkstay.payment.enums.PaymentStatus;
import blinkstay.payment.enums.ProviderEventType;
import blinkstay.payment.gateway.PaymentGateway;
import blinkstay.payment.gateway.ProviderEvent;
import blinkstay.payment.gateway.ProviderOrder;
import blinkstay.payment.repository.PaymentRepository;
import blinkstay.payment.service.PaymentEventProcessor;
import blinkstay.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepo;
    private final BookingPayments bookings;
    private final PaymentGateway gateway;
    private final PaymentEventProcessor processor;

    @Value("${app.payment.razorpay.key-id:}")
    private String razorpayKeyId;

    @Override
    public PaymentSessionDto startPayment(UUID guestId, UUID bookingId) {
        PayableBooking booking = bookings.getPayableBooking(guestId, bookingId); // 404 or 409

        // an open order for this booking is reused, so repeated clicks do not create orders
        Payment payment = paymentRepo.findFirstByBookingIdAndStatusOrderByCreatedAtDesc(bookingId, PaymentStatus.CREATED)
                .filter(p -> p.getProvider().equals(gateway.name()))
                .orElseGet(() -> createPayment(guestId, booking));

        return PaymentSessionDto.builder().paymentId(payment.getId()).bookingId(bookingId)
                .bookingReference(booking.reference()).provider(payment.getProvider())
                .orderId(payment.getProviderOrderId()).amount(payment.getAmount()).currency(payment.getCurrency())
                .mock("MOCK".equals(payment.getProvider()))
                .publicKey("RAZORPAY".equals(payment.getProvider()) ? razorpayKeyId : null)
                .holdExpiresAt(booking.holdExpiresAt()).build();
    }

    private Payment createPayment(UUID guestId, PayableBooking booking) {
        ProviderOrder order = gateway.createOrder(booking.reference(), booking.totalAmount(), booking.currency());

        return paymentRepo.save(Payment.builder().bookingId(booking.bookingId()).bookingReference(booking.reference()).guestId(guestId).provider(gateway.name()).providerOrderId(order.orderId()).amount(booking.totalAmount()).currency(booking.currency()).status(PaymentStatus.CREATED).build());
    }

    // not @Transactional: the processor opens its own transaction
    @Override
    public PaymentResultDto completeMock(UUID guestId, String orderId, MockOutcome outcome) {
        Payment p = paymentRepo.findByProviderOrderId(orderId)
                .filter(x -> x.getGuestId().equals(guestId) && "MOCK".equals(x.getProvider()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found"));

        boolean ok = outcome == MockOutcome.SUCCESS;

        // the amount comes from OUR payment record: this endpoint only chooses success or failure
        processor.process("MOCK",
                new ProviderEvent("mock_evt_" + UUID.randomUUID(),
                        ok ? ProviderEventType.PAYMENT_SUCCEEDED : ProviderEventType.PAYMENT_FAILED, orderId,
                        "mock_pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), p.getAmount(),
                        p.getCurrency(), ok ? null : "Payment declined (mock)"));

        Payment after = paymentRepo.findById(p.getId()).orElse(p);
        return PaymentResultDto.builder().paymentId(after.getId()).bookingId(after.getBookingId())
                .paymentStatus(after.getStatus()).message(messageFor(after)).build();
    }

    private static String messageFor(Payment p) {
        return switch (p.getStatus()) {
            case SUCCEEDED -> "Payment received. Your booking is confirmed.";
            case REFUNDED -> "The booking could not be kept, so your payment was refunded.";
            case REFUND_PENDING, REFUND_FAILED -> "The booking could not be kept, so your payment is being refunded.";
            case FAILED -> p.getFailureReason() != null ? p.getFailureReason() : "Payment failed. You can try again.";
            case CREATED -> "Payment is still being processed.";
        };
    }
}
