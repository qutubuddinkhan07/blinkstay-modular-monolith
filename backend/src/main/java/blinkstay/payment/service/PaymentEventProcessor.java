package blinkstay.payment.service;

import blinkstay.common.booking.BookingPayments;
import blinkstay.common.booking.PaymentConfirmation;
import blinkstay.common.booking.PaymentRefundedEvent;
import blinkstay.payment.entities.Payment;
import blinkstay.payment.entities.PaymentEvent;
import blinkstay.payment.enums.PaymentStatus;
import blinkstay.payment.gateway.PaymentGateway;
import blinkstay.payment.gateway.ProviderEvent;
import blinkstay.payment.repository.PaymentEventRepository;
import blinkstay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProcessor {
    private final PaymentRepository paymentRepo;
    private final PaymentEventRepository eventRepo;
    private final BookingPayments bookings;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher publisher;

    // A separate bean on purpose: a @Transactional method called from inside its own class would run
    // without a transaction, because the Spring proxy is skipped.
    @Transactional
    public void process(String provider, ProviderEvent event) {
        // the payment row is locked first, so two copies of the same event (or a success and a retry) run one at a time
        Payment p = paymentRepo.findByProviderOrderIdForUpdate(event.orderId())
                .filter(x -> x.getProvider().equals(provider)).orElse(null);

        if (p == null) {
            log.warn("Payment event {} for unknown order {}", event.eventId(), event.orderId());
            return;
        }

        eventRepo.save(PaymentEvent.builder().provider(provider).providerEventId(event.eventId()).type(event.type().name()).orderId(event.orderId()).build());

        if (p.getStatus() == PaymentStatus.SUCCEEDED || p.getStatus() == PaymentStatus.REFUNDED) {
            return; // final states never change again
        }

        switch (event.type()) {
            case PAYMENT_FAILED -> {
                p.setStatus(PaymentStatus.FAILED);
                p.setFailureReason(cut(event.failureReason(), 255));
                paymentRepo.save(p);
            }
            case PAYMENT_SUCCEEDED -> {
                onSucceeded(p, event);
            }
        }
    }

    private void onSucceeded(Payment p, ProviderEvent event) {
        PaymentConfirmation result = bookings.confirmPayment(p.getBookingId(), p.getId(), event.amount(),
                event.currency());

        Instant now = Instant.now();

        switch (result) {
            case CONFIRMED, ALREADY_CONFIRMED -> markPaid(p, event, now);
            case DUPLICATE -> {
                markPaid(p, event, now);
                refund(p, now, "You paid twice for this booking. The extra payment was refunded.");
            }
            case UNAVAILABLE -> {
                markPaid(p, event, now);
                refund(p, now, "The booking could not be kept: its hold expired and the rooms were no longer free, or" +
                        " " +
                        "it was cancelled.");
            }
            case MISMATCH -> {
                // money that dose not match the booking is never confirmend automatically
                p.setStatus(PaymentStatus.FAILED);
                p.setFailureReason("The amount paid did not match the booking total");
                paymentRepo.save(p);

                log.error("Payment {} for booking {} does not match the booking total (paid {} {}. Needs review.",
                        p.getId(), p.getBookingReference(), event.amount(), event.currency());
            }
        }
    }

    private void markPaid(Payment p, ProviderEvent event, Instant now) {
        p.setStatus(PaymentStatus.SUCCEEDED);
        p.setProviderPaymentId(event.providerPaymentId());
        p.setPaidAt(now);
        paymentRepo.save(p);
    }

    private void refund(Payment p, Instant now, String reason) {
        gateway.refund(p.getProviderPaymentId(), p.getAmount(), reason);
        p.setStatus(PaymentStatus.REFUNDED);
        p.setRefundedAmount(p.getAmount());
        p.setRefundedAt(now);
        paymentRepo.save(p);
        publisher.publishEvent(new PaymentRefundedEvent(p.getId(), p.getBookingId(), p.getGuestId(),
                p.getBookingReference(), p.getAmount(), p.getCurrency(), reason));
    }

    private static String cut(String s, int max) {
        return s == null ? "Payment failed" : (s.length() <= max ? s : s.substring(0, max));
    }
}
