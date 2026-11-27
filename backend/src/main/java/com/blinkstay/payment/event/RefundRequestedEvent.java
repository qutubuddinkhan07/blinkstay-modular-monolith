package com.blinkstay.payment.event;

import java.util.UUID;

public record RefundRequestedEvent(UUID paymentId) {
}
