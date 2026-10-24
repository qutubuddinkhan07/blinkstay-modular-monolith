package blinkstay.room.entities;

import blinkstay.room.enums.RoomCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "listing_rooms", indexes = {
        @Index(name = "idx_listings_id", columnList = "listingId")}, uniqueConstraints = @UniqueConstraint(name = "uk_listing_room_type", columnNames = {
        "listing_id", "room_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ListingRoom {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME) // Generates time order sequential UUIDs
    @JdbcTypeCode(SqlTypes.BINARY) // Maps java.util.UUID directly to MySQL BINARY(16)
    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID listingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomCategory roomType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer totalRooms;

    @Builder.Default
    @Column(name = "max_guests", nullable = false, columnDefinition = "INT DEFAULT 2")
    private Integer maxGuests = 2;
}
