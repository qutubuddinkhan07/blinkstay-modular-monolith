package blinkstay.payment.gateway;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Exists only with the prod profile AND the mock provider. Production refuses mock
 * payments unless the deployment opts in on purpose (a demo when Razorpay is down).
 */
@Slf4j
@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProdGuard {

    @Value("${app.payment.allow-mock-in-prod:false}")
    private boolean allowMockInProd;

    @PostConstruct
    void check() {
        if (!allowMockInProd) {
            throw new IllegalStateException("Mock payments must not run with the prod profile. "
                    + "Set PAYMENT_PROVIDER=razorpay, or ALLOW_MOCK_PAYMENTS=true for a deliberate demo.");
        }
        log.warn("DEMO MODE: mock payments are enabled in production. No real money is taken.");
    }
}