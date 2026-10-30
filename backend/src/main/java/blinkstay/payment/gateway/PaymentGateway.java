package blinkstay.payment.gateway;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentGateway {
    String name();

    ProviderOrder createOrder(String reference, BigDecimal amount, String currency);

    ProviderRefund refund(String providerPaymentId, BigDecimal amount, String reason);

    /**
     * A captured payment for this order at the provider, if any. Used by the reconcile job.
     */
    default Optional<ProviderCapture> findCapturedPayment(String orderId) {
        return Optional.empty();
    }
}
