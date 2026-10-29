package blinkstay.payment.serviceImpl;

import blinkstay.common.booking.PaymentRefundedEvent;
import blinkstay.payment.entities.Payment;
import blinkstay.payment.enums.PaymentStatus;
import blinkstay.payment.gateway.PaymentGateway;
import blinkstay.payment.gateway.ProviderRefund;
import blinkstay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sends refunds to the provider OUTSIDE any database transaction.
 * Not @Transactional on purpose: each short transaction below is opened with the TransactionTemplate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundProcessor {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LEASE = Duration.ofMinutes(5);

    private final PaymentRepository paymentRepo;
    private final PaymentGateway gateway;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher publisher;

    private record Claim(String providerPaymentId, BigDecimal amount, String reason) {
    }

    /**
     * Called by the timer: tries every refund that is due.
     */
    public int runDue() {
        List<UUID> ids = paymentRepo.findDueRefundIds(Instant.now(), PageRequest.of(0, 20));
        ids.forEach(this::attempt);
        return ids.size();
    }

    /**
     * One refund attempt. Never throws.
     */
    public void attempt(UUID paymentId) {
        try {
            Claim claim = tx.execute(status -> claim(paymentId));
            if (claim == null) {
                return; // not pending, not due yet, or another worker has it
            }

            ProviderRefund refund;
            try {
                refund = gateway.refund(claim.providerPaymentId(), claim.amount(), claim.reason());
            } catch (Exception e) {
                recordFailure(paymentId, e);
                return;
            }
            recordSuccess(paymentId);
        } catch (Exception e) {
            log.error("Refund attempt for payment {} crashed", paymentId, e);
        }
    }

    // STEP 1: lock the row, count the attempt and set a lease so nobody else takes it meanwhile
    private Claim claim(UUID paymentId) {
        Payment p = paymentRepo.findByIdForUpdate(paymentId).orElse(null);
        if (p == null || p.getStatus() != PaymentStatus.REFUND_PENDING) {
            return null;
        }

        Instant now = Instant.now();
        if (p.getNextRefundAt() != null && p.getNextRefundAt().isAfter(now)) {
            return null;
        }

        p.setRefundAttempts(attempts(p) + 1);
        p.setNextRefundAt(now.plus(LEASE));
        paymentRepo.save(p);
        return new Claim(p.getProviderPaymentId(), p.getAmount(), p.getRefundReason());
    }

    // STEP 3a: the provider accepted the refund
    private void recordSuccess(UUID paymentId) {
        tx.executeWithoutResult(status -> {
            Payment p = paymentRepo.findByIdForUpdate(paymentId).orElse(null);
            if (p == null || p.getStatus() != PaymentStatus.REFUND_PENDING) {
                return;
            }

            p.setStatus(PaymentStatus.REFUNDED);
            p.setRefundedAmount(p.getAmount());
            p.setRefundedAt(Instant.now());
            p.setNextRefundAt(null);
            p.setRefundError(null);
            paymentRepo.save(p);

            log.info("Payment {} refunded in full ({} {})", p.getId(), p.getAmount(), p.getCurrency());

            if (Boolean.TRUE.equals(p.getRefundNotify())) {
                publisher.publishEvent(new PaymentRefundedEvent(p.getId(), p.getBookingId(), p.getGuestId(),
                        p.getBookingReference(), p.getAmount(), p.getCurrency(), p.getRefundReason()));
            }
        });
    }

    // STEP 3b: the provider call failed: eait and retry, or give up after MAX_ATTEMPTS
    private void recordFailure(UUID paymentId, Exception e) {
        tx.executeWithoutResult(status -> {
            Payment p = paymentRepo.findByIdForUpdate(paymentId).orElse(null);
            if (p == null || p.getStatus() != PaymentStatus.REFUND_PENDING) {
                return;
            }

            int attempts = attempts(p);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            p.setRefundError(msg.length() <= 255 ? msg : msg.substring(0, 255));

            if (attempts >= MAX_ATTEMPTS) {
                p.setStatus(PaymentStatus.REFUND_FAILED);
                p.setNextRefundAt(null);
                log.error("Refund for payment{} (booking {}) failed {} times and needs attention: {}", p.getId(),
                        p.getBookingReference(), attempts, msg);
            } else {
                p.setNextRefundAt(Instant.now().plus(backoff(attempts)));
                log.warn("Refund for payment {} failed (attempt {}), will retry: {}", p.getId(), attempts, msg);
            }
            paymentRepo.save(p);
        });
    }

    private static int attempts(Payment p) {
        return p.getRefundAttempts() == null ? 0 : p.getRefundAttempts();
    }

    private static Duration backoff(int attempts) {
        return switch (attempts) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            case 3 -> Duration.ofMinutes(15);
            default -> Duration.ofHours(1);
        };
    }
}
