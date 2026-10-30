package blinkstay.payment.repository;

import blinkstay.payment.entities.Payment;
import blinkstay.payment.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findFirstByBookingIdAndStatusOrderByCreatedAtDesc(UUID bookingId, PaymentStatus status);

    Optional<Payment> findByProviderOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.providerOrderId = :orderId")
    Optional<Payment> findByProviderOrderIdForUpdate(@Param("orderId") String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

    @Query("select p.id from Payment p where p.status = blinkstay.payment.enums.PaymentStatus.REFUND_PENDING"
            + " and (p.nextRefundAt is null or p.nextRefundAt <= :now) order by p.nextRefundAt")
    List<UUID> findDueRefundIds(@Param("now") Instant now, Pageable pageable);

    @Query("select p from Payment p where p.status = blinkstay.payment.enums.PaymentStatus.REFUND_FAILED"
            + " or p.reviewReason is not null order by p.updatedAt desc")
    List<Payment> findNeedingAttention(Pageable pageable);

    @Query("select p from Payment p where p.provider = :provider"
            + " and p.status = blinkstay.payment.enums.PaymentStatus.CREATED"
            + " and p.createdAt > :after and p.createdAt < :before order by p.createdAt")
    List<Payment> findStaleCreated(@Param("provider") String provider, @Param("after") Instant after,
                                   @Param("before") Instant before, Pageable pageable);
}
