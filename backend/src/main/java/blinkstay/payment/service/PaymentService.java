package blinkstay.payment.service;

import blinkstay.payment.dto.PaymentResultDto;
import blinkstay.payment.dto.PaymentSessionDto;
import blinkstay.payment.enums.MockOutcome;

import java.util.UUID;

public interface PaymentService {
    PaymentSessionDto startPayment(UUID guestId, UUID bookingId);

    PaymentResultDto completeMock(UUID guestId, String orderId, MockOutcome outcome);
}
