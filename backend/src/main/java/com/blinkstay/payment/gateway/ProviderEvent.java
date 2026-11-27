package com.blinkstay.payment.gateway;

import com.blinkstay.payment.enums.ProviderEventType;

import java.math.BigDecimal;

public record ProviderEvent(String eventId, ProviderEventType type, String orderId, String providerPaymentId,
                            BigDecimal amount,
                            String currency, String failureReason) {
}
