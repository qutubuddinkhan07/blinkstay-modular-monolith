package com.blinkstay.payment.service;

import com.blinkstay.payment.dto.PaymentResultDto;
import com.blinkstay.payment.dto.PaymentSessionDto;
import com.blinkstay.payment.enums.MockOutcome;

import java.util.UUID;

public interface PaymentService {
    PaymentSessionDto startPayment(UUID guestId, UUID bookingId);

    PaymentResultDto completeMock(UUID guestId, String orderId, MockOutcome outcome);
}
