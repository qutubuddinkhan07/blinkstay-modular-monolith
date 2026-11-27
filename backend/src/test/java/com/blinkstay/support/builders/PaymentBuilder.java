package com.blinkstay.support.builders;

import com.blinkstay.payment.entities.Payment;
import com.blinkstay.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Builds a valid Payment, with an id already set (unit tests use it without saving):
 * <p>
 * Payment p = aPayment().status(PaymentStatus.SUCCEEDED).build();
 * <p>
 * Defaults: provider RAZORPAY, status CREATED, 4000.00 INR, random ids and order id.
 */
public class PaymentBuilder {

    private UUID id = UUID.randomUUID();
    private UUID bookingId = UUID.randomUUID();
    private String bookingReference = "TPAY00001";
    private UUID guestId = UUID.randomUUID();
    private String provider = "RAZORPAY";
    private String providerOrderId = "order_" + UUID.randomUUID().toString().substring(0, 8);
    private BigDecimal amount = new BigDecimal("4000.00");
    private String currency = "INR";
    private PaymentStatus status = PaymentStatus.CREATED;

    private PaymentBuilder() {
    }

    public static PaymentBuilder aPayment() {
        return new PaymentBuilder();
    }

    public PaymentBuilder booking(UUID bookingId) {
        this.bookingId = bookingId;
        return this;
    }

    public PaymentBuilder provider(String provider) {
        this.provider = provider;
        return this;
    }

    public PaymentBuilder orderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
        return this;
    }

    public PaymentBuilder amount(String amount) {
        this.amount = new BigDecimal(amount);
        return this;
    }

    public PaymentBuilder status(PaymentStatus status) {
        this.status = status;
        return this;
    }

    public Payment build() {
        return Payment.builder()
                .id(id)
                .bookingId(bookingId)
                .bookingReference(bookingReference)
                .guestId(guestId)
                .provider(provider)
                .providerOrderId(providerOrderId)
                .amount(amount)
                .currency(currency)
                .status(status)
                .build();
    }
}
