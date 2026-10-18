package blinkstay.payment.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_events", uniqueConstraints = @UniqueConstraint(name = "uk_payment_event",
        columnNames = {"provider", "provider_event_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String provider;
    @Column(nullable = false, length = 100)
    private String providerEventId;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(nullable = false, length = 64)
    private String orderId;
    @CreationTimestamp
    private Instant receivedAt;
}
