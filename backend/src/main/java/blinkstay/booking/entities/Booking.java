package blinkstay.booking.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import blinkstay.booking.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "bookings", indexes = { @Index(name = "idx_booking_guest", columnList = "guestId,createdAt"),
		@Index(name = "idx_booking_room_dates", columnList = "roomId,checkIn,checkOut"),
		@Index(name = "idx_booking_listing", columnList = "listingId"),
		@Index(name = "idx_booking_host", columnList = "hostId"),
		@Index(name = "idx_booking_status_hold", columnList = "status,holdExpiresAt") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Booking {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	@JdbcTypeCode(SqlTypes.BINARY)
	@Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "booking_ref", nullable = false, unique = true, length = 12)
	private String reference;

	@Column(nullable = false)
	private UUID guestId;
	@Column(nullable = false)
	private UUID hostId;
	@Column(nullable = false)
	private UUID listingId;
	@Column(nullable = false)
	private UUID roomId;

	@Column(nullable = false)
	private LocalDate checkIn;
	@Column(nullable = false)
	private LocalDate checkOut;
	@Column(nullable = false)
	private int nights;
	@Column(nullable = false)
	private int roomsBooked;
	@Column(nullable = false)
	private int guests;

	// VARCHAR, so adding a status later never needs a column change
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private BookingStatus status;

	// price snapshot: later price edits never change an existing booking
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal pricePerNight;
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;
	@Column(nullable = false, length = 3)
	private String currency;

	// timestamps are UTC instants, so the browser shows the right countdown in any
	// time zone
	private Instant holdExpiresAt;
	private Instant confirmedAt;
	private Instant cancelledAt;
	private UUID cancelledBy;
	@Column(length = 500)
	private String cancellationReason;
	@Column(precision = 12, scale = 2)
	private BigDecimal refundAmount;

	// snapshot of what was booked: a past booking still reads correctly if the
	// listing is deleted
	@Column(nullable = false)
	private String listingTitle;
	private String listingLocation;
	@Column(length = 500)
	private String coverImageUrl;
	@Column(nullable = false, length = 30)
	private String roomType;

	@Version
	private Long version;

	@CreationTimestamp
	private Instant createdAt;
	@UpdateTimestamp
	private Instant updatedAt;
}