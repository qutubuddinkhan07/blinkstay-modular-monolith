package com.blinkstay.payment.repository;

import com.blinkstay.payment.entities.Payment;
import com.blinkstay.payment.enums.PaymentStatus;
import com.blinkstay.support.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.blinkstay.payment.enums.PaymentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
@DisplayName("PaymentRepository on real Postgres")
class PaymentRepositoryIT {

    private static final Instant NOW = Instant.parse("2030-01-15T06:30:00Z");
    private static final PageRequest FIRST_10 = PageRequest.of(0, 10);

    @Autowired
    private PaymentRepository repo;

    @Autowired
    private EntityManager em;

    // ---------- helpers ----------

    /**
     * A valid, unsaved payment; the id is left empty so Hibernate inserts it.
     */
    private Payment newPayment(PaymentStatus status) {
        return Payment.builder()
                .bookingId(UUID.randomUUID()).bookingReference("T123456789").guestId(UUID.randomUUID())
                .provider("RAZORPAY").providerOrderId("order_" + UUID.randomUUID())
                .amount(new BigDecimal("4000.00")).currency("INR")
                .status(status)
                .build();
    }

    private Payment saved(PaymentStatus status) {
        return repo.saveAndFlush(newPayment(status));
    }

    private Payment refundPending(Instant nextRefundAt) {
        Payment payment = newPayment(REFUND_PENDING);
        payment.setNextRefundAt(nextRefundAt);
        return repo.saveAndFlush(payment);
    }

    /**
     * Hibernate fills createdAt with the real time; tests that filter on it set it by hand.
     */
    private void setCreatedAt(Payment payment, Instant at) {
        em.createQuery("update Payment p set p.createdAt = :at where p.id = :id")
                .setParameter("at", at)
                .setParameter("id", payment.getId())
                .executeUpdate();
        em.clear();
    }

    // ---------- refund queue ----------

    @Nested
    @DisplayName("findDueRefundIds")
    class FindDueRefundIds {

        @Test
        @DisplayName("returns refunds whose time has come, earliest first, never-tried (null) last")
        void returnsDueRefunds_earliestFirst_nullLast() {
            Payment tenMinutesAgo = refundPending(NOW.minus(Duration.ofMinutes(10)));
            Payment hourAgo = refundPending(NOW.minus(Duration.ofHours(1)));
            Payment neverTried = refundPending(null);
            refundPending(NOW.plus(Duration.ofMinutes(5)));                 // retry not due yet
            Payment failed = newPayment(REFUND_FAILED);                     // gave up: not in the queue
            failed.setNextRefundAt(NOW.minus(Duration.ofHours(1)));
            repo.saveAndFlush(failed);

            List<UUID> due = repo.findDueRefundIds(NOW, FIRST_10);

            // Postgres sorts NULL last in ascending order (H2 and MySQL put it first)
            assertThat(due).containsExactly(hourAgo.getId(), tenMinutesAgo.getId(), neverTried.getId());
        }

        @Test
        void refundDueExactlyNow_isIncluded() {
            Payment dueNow = refundPending(NOW);

            assertThat(repo.findDueRefundIds(NOW, FIRST_10)).containsExactly(dueNow.getId());
        }

        @Test
        void pageSize_limitsTheBatch() {
            refundPending(NOW.minus(Duration.ofMinutes(1)));
            refundPending(NOW.minus(Duration.ofMinutes(2)));
            refundPending(NOW.minus(Duration.ofMinutes(3)));

            assertThat(repo.findDueRefundIds(NOW, PageRequest.of(0, 2))).hasSize(2);
        }
    }

    // ---------- reconciliation of unpaid orders ----------

    @Nested
    @DisplayName("findStaleCreated")
    class FindStaleCreated {

        private final Instant after = NOW.minus(Duration.ofHours(2));
        private final Instant before = NOW.minus(Duration.ofMinutes(15));

        @Test
        void returnsOnlyCreatedPaymentsOfTheProviderInsideTheWindow() {
            Payment stale = saved(CREATED);
            setCreatedAt(stale, NOW.minus(Duration.ofMinutes(30)));
            Payment tooNew = saved(CREATED);
            setCreatedAt(tooNew, NOW.minus(Duration.ofMinutes(5)));
            Payment tooOld = saved(CREATED);
            setCreatedAt(tooOld, NOW.minus(Duration.ofHours(3)));
            Payment paid = saved(SUCCEEDED);
            setCreatedAt(paid, NOW.minus(Duration.ofMinutes(30)));
            Payment otherProvider = newPayment(CREATED);
            otherProvider.setProvider("MOCK");
            otherProvider = repo.saveAndFlush(otherProvider);
            setCreatedAt(otherProvider, NOW.minus(Duration.ofMinutes(30)));

            List<Payment> found = repo.findStaleCreated("RAZORPAY", after, before, FIRST_10);

            assertThat(found).extracting(Payment::getId).containsExactly(stale.getId());
        }

        @Test
        void returnsOldestFirst() {
            Payment newer = saved(CREATED);
            setCreatedAt(newer, NOW.minus(Duration.ofMinutes(30)));
            Payment older = saved(CREATED);
            setCreatedAt(older, NOW.minus(Duration.ofMinutes(60)));

            List<Payment> found = repo.findStaleCreated("RAZORPAY", after, before, FIRST_10);

            assertThat(found).extracting(Payment::getId).containsExactly(older.getId(), newer.getId());
        }
    }

    // ---------- admin view ----------

    @Test
    @DisplayName("findNeedingAttention / countNeedingAttention: failed refunds and payments flagged for review")
    void needingAttention_isFailedRefundsAndFlaggedPayments() {
        Payment failed = saved(REFUND_FAILED);
        Payment flagged = newPayment(SUCCEEDED);
        flagged.setReviewReason("Paid after the hold expired");
        flagged = repo.saveAndFlush(flagged);
        saved(SUCCEEDED);
        saved(REFUNDED);

        assertThat(repo.findNeedingAttention(FIRST_10)).extracting(Payment::getId)
                .containsExactlyInAnyOrder(failed.getId(), flagged.getId());
        assertThat(repo.countNeedingAttention()).isEqualTo(2);
    }

    // ---------- lookups ----------

    @Test
    void findByProviderOrderIdForUpdate_findsTheRowByOrderId() {
        Payment payment = saved(CREATED);

        assertThat(repo.findByProviderOrderIdForUpdate(payment.getProviderOrderId()))
                .map(Payment::getId).contains(payment.getId());
        assertThat(repo.findByProviderOrderIdForUpdate("order_unknown")).isEmpty();
    }

    @Test
    void findFirstByBookingIdAndStatus_returnsTheNewestPaymentWithThatStatus() {
        UUID bookingId = UUID.randomUUID();
        Payment older = newPayment(SUCCEEDED);
        older.setBookingId(bookingId);
        older = repo.saveAndFlush(older);
        setCreatedAt(older, NOW.minus(Duration.ofHours(2)));
        Payment newer = newPayment(SUCCEEDED);
        newer.setBookingId(bookingId);
        newer = repo.saveAndFlush(newer);
        setCreatedAt(newer, NOW.minus(Duration.ofHours(1)));
        Payment failedNewest = newPayment(FAILED);
        failedNewest.setBookingId(bookingId);
        failedNewest = repo.saveAndFlush(failedNewest);
        setCreatedAt(failedNewest, NOW);

        assertThat(repo.findFirstByBookingIdAndStatusOrderByCreatedAtDesc(bookingId, SUCCEEDED))
                .map(Payment::getId).contains(newer.getId());
    }

    // ---------- unique keys ----------

    @Nested
    @DisplayName("unique keys")
    class UniqueKeys {

        @Test
        void sameProviderOrderIdTwice_isRejected() {
            Payment first = saved(CREATED);
            Payment copy = newPayment(CREATED);
            copy.setProviderOrderId(first.getProviderOrderId());

            assertThatThrownBy(() -> repo.saveAndFlush(copy))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("many payments may have no providerPaymentId yet (Postgres allows several NULLs in a unique column)")
        void manyPaymentsWithoutProviderPaymentId_areAllowed() {
            saved(CREATED);
            saved(CREATED);

            assertThat(repo.count()).isEqualTo(2);
        }

        @Test
        void sameProviderPaymentIdTwice_isRejected() {
            Payment first = newPayment(SUCCEEDED);
            first.setProviderPaymentId("pay_test_123");
            repo.saveAndFlush(first);
            Payment copy = newPayment(SUCCEEDED);
            copy.setProviderPaymentId("pay_test_123");

            assertThatThrownBy(() -> repo.saveAndFlush(copy))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}