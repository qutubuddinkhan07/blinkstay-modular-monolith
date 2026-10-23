package blinkstay.payment.serviceImpl;

import blinkstay.common.payment.PaymentRefunds;
import blinkstay.payment.entities.Payment;
import blinkstay.payment.enums.PaymentStatus;
import blinkstay.payment.gateway.PaymentGateway;
import blinkstay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final PaymentGateway gateway;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockPayment(UUID paymentId) {
        paymentRepo.findByIdForUpdate(paymentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Payment not found"));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal refundInFull(UUID paymentId, String reason) {
        Payment p = paymentRepo.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (p.getStatus() == PaymentStatus.REFUNDED) {
            return p.getRefundedAmount();
        }

        if (p.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment cannot be refunded");
        }

        gateway.refund(p.getProviderPaymentId(), p.getAmount(), reason);
        p.setStatus(PaymentStatus.REFUNDED);
        p.setRefundedAmount(p.getAmount());
        p.setRefundedAt(Instant.now());
        paymentRepo.save(p);

        log.info("Payment {} refund in full ({} {})", p.getId(), p.getAmount(), p.getCurrency());

        return p.getAmount();
    }
}
