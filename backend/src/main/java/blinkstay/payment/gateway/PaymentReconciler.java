package blinkstay.payment.gateway;

import blinkstay.payment.entities.Payment;
import blinkstay.payment.enums.ProviderEventType;
import blinkstay.payment.repository.PaymentRepository;
import blinkstay.payment.service.PaymentEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Safety net for a payment that succeeded at the provider but whose confirmation never reached us
 * (browser closed before verify, webhook missed). Not @Transactional: the processor opens its own transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciler {
    private final PaymentRepository paymentRepo;
    private final PaymentGateway gateway;
    private final PaymentEventProcessor processor;

    @Value("${app.payment.reconcile.min-age-minutes:5}")
    private long minAgeMinutes;
    @Value("${app.payment.reconcile.max-age-minutes:120}")
    private long maxAgeMinutes;

    public int run() {
        Instant now = Instant.now();
        List<Payment> stale = paymentRepo.findStaleCreated(gateway.name(),
                now.minus(Duration.ofMinutes(maxAgeMinutes)), now.minus(Duration.ofMinutes(minAgeMinutes)),
                PageRequest.of(0, 20));

        int fixed = 0;
        for (Payment p : stale) {
            try {
                Optional<ProviderCapture> captured = gateway.findCapturedPayment(p.getProviderOrderId());
                if (captured.isEmpty()) {
                    continue;
                }

                ProviderCapture c = captured.get();
                // amount and currency come from the provider here, so the mismatch check is a real check
                processor.process(gateway.name(), new ProviderEvent("reconcile_" + c.paymentId(),
                        ProviderEventType.PAYMENT_SUCCEEDED, p.getProviderOrderId(), c.paymentId(), c.amount(),
                        c.currency(), null));
                fixed++;
                log.info("Reconciled payment {} (booking {}) from ptovider payment {}", p.getId(),
                        p.getBookingReference(), c.paymentId());
            } catch (Exception e) {
                log.warn("Could not reconcile payment {}: {}", p.getId(), e.getMessage());
            }
        }

        return fixed;
    }
}
