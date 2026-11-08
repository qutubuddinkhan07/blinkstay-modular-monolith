package blinkstay.payment.serviceImpl;

import blinkstay.common.stats.PaymentStats;
import blinkstay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentStatsImpl implements PaymentStats {
    private final PaymentRepository paymentRepo;

    @Override
    @Transactional(readOnly = true)
    public long needingAttention() {
        return paymentRepo.countNeedingAttention();
    }
}
