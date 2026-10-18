package blinkstay.payment.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProdGuard {
    void check() {
        throw new IllegalArgumentException("Mock payments must not run with the prod profile. Set app.payment" +
                ".provider");
    }
}
