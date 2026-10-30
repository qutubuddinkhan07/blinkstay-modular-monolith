package blinkstay.payment.entities;

import blinkstay.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_booking", columnList = "bookingId, status"),
        @Index(name = "idx_payment_guest", columnList = "guestId, createdAt"),
        @Index(name = "idx_payment_refund", columnList = "status, nextRefundAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Payment {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID bookingId;
    @Column(nullable = false, length = 12)
    private String bookingReference;
    @Column(nullable = false)
    private UUID guestId;

    @Column(nullable = false, length = 20)
    private String provider;
    @Column(nullable = false, unique = true, length = 64)
    private String providerOrderId;
    @Column(unique = true, length = 64)
    private String providerPaymentId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(precision = 12, scale = 2)
    private BigDecimal refundedAmount;
    @Column(length = 255)
    private String failureReason;
    private Instant paidAt;
    private Instant refundedAt;

    @Column(length = 255)
    private String refundReason;
    private Integer refundAttempts;
    private Instant nextRefundAt;
    @Column(length = 255)
    private String refundError;
    private Boolean refundNotify; // true: send the guest a "payment refunded email" when done
    @Column(length = 255)
    private String reviewReason;

    @Version
    private Long version;
    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
