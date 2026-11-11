package blinkstay.earnings.entities;

import blinkstay.earnings.enums.EarningsStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "earnings_entries", indexes = {
        @Index(name = "idx_earn_host_checkin", columnList = "hostId, checkIn"),
        @Index(name = "idx_earn_status_checkout", columnList = "status, checkOut"),
        @Index(name = "idx_earn_sold_on", columnList = "soldOn")
})
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EarningsEntry {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true) // one row per booking
    private UUID bookingId;
    @Column(nullable = false, length = 12)
    private String bookingReference;
    @Column(nullable = false)
    private UUID hostId;
    @Column(nullable = false)
    private UUID listingId;
    @Column(nullable = false)
    private String listingTitle;
    @Column(nullable = false)
    private LocalDate checkIn;
    @Column(nullable = false)
    private LocalDate checkOut;
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gross;   // what the guest paid
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refunded;    // what was given back after a cancel
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercent;   // saved, so a later change never rewrited old rows
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commission;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal hostAmount;  // gross - refunded - commission

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EarningsStatus status;

    private Instant earnedAt;
    private LocalDate soldOn; // the hotel-zone date the booking was confirmed (admin reports filter on this)

    @Version
    private Long version;
    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
