package blinkstay.payment.gateway;

import java.math.BigDecimal;

public record ProviderCapture(String paymentId, BigDecimal amount, String currency) {
}
