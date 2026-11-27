package com.blinkstay.payment.service;

import com.blinkstay.payment.dto.AdminPaymentDto;

import java.util.List;
import java.util.UUID;

public interface AdminPaymentService {
    List<AdminPaymentDto> needingAttention();

    AdminPaymentDto retryRefund(UUID paymentId);

    AdminPaymentDto resolve(UUID paymentId);
}
