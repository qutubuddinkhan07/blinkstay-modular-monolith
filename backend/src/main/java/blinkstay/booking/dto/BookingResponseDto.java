package blinkstay.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import blinkstay.booking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDto {
	private UUID id;
	private String reference;
	private UUID listingId;
	private UUID roomId;
	private String listingTitle;
	private String listingLocation;
	private String coverImageUrl;
	private String roomType;
	private LocalDate checkIn;
	private LocalDate checkOut;
	private int nights;
	private int rooms;
	private int guests;
	private BigDecimal pricePerNight;
	private BigDecimal totalAmount;
	private String currency;
	private BookingStatus status;
	private Instant holdExpiresAt;
	private boolean payable; // waiting for payment AND the hold has not run out
	private Instant confirmedAt;
	private Instant cancelledAt;
	private String cancellationReason;
	private BigDecimal refundAmount;
	private Instant createdAt;
}
