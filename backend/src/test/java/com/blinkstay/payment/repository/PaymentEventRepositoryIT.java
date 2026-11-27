package com.blinkstay.payment.repository;

import com.blinkstay.payment.entities.PaymentEvent;
import com.blinkstay.support.RepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
@DisplayName("PaymentEventRepository on real Postgres")
class PaymentEventRepositoryIT {

    @Autowired
    private PaymentEventRepository repo;

    private PaymentEvent event(String provider, String providerEventId) {
        return PaymentEvent.builder()
                .provider(provider).providerEventId(providerEventId)
                .type("payment.captured").orderId("order_test_1")
                .build();
    }

    @Test
    void existsByProviderAndProviderEventId_findsOnlyTheStoredEvent() {
        repo.saveAndFlush(event("RAZORPAY", "evt_1"));

        assertThat(repo.existsByProviderAndProviderEventId("RAZORPAY", "evt_1")).isTrue();
        assertThat(repo.existsByProviderAndProviderEventId("RAZORPAY", "evt_2")).isFalse();
        assertThat(repo.existsByProviderAndProviderEventId("MOCK", "evt_1")).isFalse();
    }

    @Test
    @DisplayName("the same event delivered twice is rejected by uk_payment_event (last line of webhook idempotency)")
    void sameEventTwice_isRejectedByTheDatabase() {
        repo.saveAndFlush(event("RAZORPAY", "evt_1"));

        assertThatThrownBy(() -> repo.saveAndFlush(event("RAZORPAY", "evt_1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameEventIdFromAnotherProvider_isAllowed() {
        repo.saveAndFlush(event("RAZORPAY", "evt_1"));
        repo.saveAndFlush(event("MOCK", "evt_1"));

        assertThat(repo.count()).isEqualTo(2);
    }
}