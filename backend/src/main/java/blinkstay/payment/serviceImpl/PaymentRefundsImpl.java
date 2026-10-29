package blinkstay.payment.serviceImpl;

import blinkstay.common.payment.PaymentRefunds;
import blinkstay.payment.entities.Payment;
import blinkstay.payment.enums.PaymentStatus;
import blinkstay.payment.event.RefundRequestedEvent;
import blinkstay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundsImpl implements PaymentRefunds {
    private final PaymentRepository paymentRepo;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockPayment(UUID paymentId) {
        paymentRepo.findByIdForUpdate(paymentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Payment not found"));
    }

    /**
     * Only RECORDS the refund request. The money is sent by RefundProcessor after this transaction commits.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal refundInFull(UUID paymentId, String reason) {
        Payment p = paymentRepo.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        switch (p.getStatus()) {
            case REFUNDED, REFUND_PENDING, REFUND_FAILED -> {
                return p.getAmount(); // already asked for
            }
            case SUCCEEDED -> {
                // continue below
            }
            default -> throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment cannot be refunded");
        }

        p.setStatus(PaymentStatus.REFUND_PENDING);
        p.setRefundReason(reason != null && reason.length() > 255 ? reason.substring(0, 255) : reason);
        p.setRefundNotify(false); // the cancellation email already tells the guest about the refund
        p.setRefundAttempts(0);
        p.setNextRefundAt(Instant.now());
        paymentRepo.save(p);

        publisher.publishEvent(new RefundRequestedEvent(p.getId()));

        log.info("Refund requested for payment {} ({} {})", p.getId(), p.getAmount(), p.getCurrency());

        return p.getAmount();
    }
}