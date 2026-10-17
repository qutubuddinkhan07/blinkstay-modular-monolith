package blinkstay.booking.dto;

import java.math.BigDecimal;
import java.util.UUID;

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
public class RoomAvailabilityDto {
	private UUID roomId;
	private String roomType;
	private BigDecimal pricePerNight;
	private int maxGuests;
	private int totalRooms;
	private int freeRooms; // free on EVERY night of the requested stay
	private BigDecimal totalPrice; // for the requested number of rooms
	private boolean bookable;
	private String reason; // why it is not bookable, otherwise null
}
