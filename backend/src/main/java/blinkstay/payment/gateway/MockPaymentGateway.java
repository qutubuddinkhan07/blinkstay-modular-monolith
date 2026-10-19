package blinkstay.payment.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public String name() {
        return "MOCK";
    }

    @Override
    public ProviderOrder createOrder(String receipt, BigDecimal amount, String currency) {
        return new ProviderOrder("mock_order_" + shortId());
    }

    @Override
    public ProviderRefund refund(String providerPaymentId, BigDecimal amount, String reason) {
        return new ProviderRefund("mock_refund_" + shortId());
    }

    private static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
