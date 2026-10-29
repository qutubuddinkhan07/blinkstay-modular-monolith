package blinkstay.payment.serviceImpl;

import blinkstay.payment.dto.PaymentResultDto;
import blinkstay.payment.dto.RazorpayVerifyDto;
import blinkstay.payment.entities.Payment;
import blinkstay.payment.enums.ProviderEventType;
import blinkstay.payment.gateway.ProviderEvent;
import blinkstay.payment.gateway.RazorpaySignature;
import blinkstay.payment.repository.PaymentRepository;
import blinkstay.payment.service.PaymentEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "razorpay")
public class RazorpayCheckoutService {
    private final PaymentRepository paymentRepo;
    private final PaymentEventProcessor processor;

    @Value("${app.payment.razorpay.key-secret}")
    private String keySecret;

    // not @Transactional: the processor opens its own transaction
    public PaymentResultDto verify(UUID guestId, String orderId, RazorpayVerifyDto dto) {
        Payment p = paymentRepo.findByProviderOrderId(orderId)
                .filter(x -> x.getGuestId().equals(guestId) && "RAZORPAY".equals(x.getProvider()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found"));

        // Razorpay signs "orderId|paymentId" with our key secret: it proves this payment belongs to this order
        byte[] signed = (orderId + "|" + dto.getRazorpayPaymentId()).getBytes(StandardCharsets.UTF_8);
        if (!RazorpaySignature.valid(signed, dto.getRazorpaySignature(), keySecret)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment could not be verified");
        }

        // the amount comes from our record: the order was created with it and Razorpay cannot charge another amount
        processor.process("RAZORPAY", new ProviderEvent("verify_" + dto.getRazorpayPaymentId(),
                ProviderEventType.PAYMENT_SUCCEEDED, orderId, dto.getRazorpayPaymentId(), p.getAmount(),
                p.getCurrency(), null));

        Payment after = paymentRepo.findById(p.getId()).orElse(p);
        return PaymentResultDto.builder().paymentId(after.getId())
                .bookingId(after.getBookingId())
                .paymentStatus(after.getStatus())
                .message(messageFor(after)).build();
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
