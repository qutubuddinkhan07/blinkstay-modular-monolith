package blinkstay.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {
    String name();

    ProviderOrder createOrder(String receipt, BigDecimal amount, String currency);

    ProviderRefund refund(String providerPaymentId, BigDecimal amount, String reason);
}
