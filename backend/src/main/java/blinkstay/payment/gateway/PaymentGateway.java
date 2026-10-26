package blinkstay.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {
    String name();

    ProviderOrder createOrder(String reference, BigDecimal amount, String currency);

    ProviderRefund refund(String providerPaymentId, BigDecimal amount, String reason);
}
