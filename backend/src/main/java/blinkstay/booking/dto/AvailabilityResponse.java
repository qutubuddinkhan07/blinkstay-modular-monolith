package blinkstay.booking.dto;

import java.time.LocalDate;
import java.util.List;
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
public class AvailabilityResponse {
	private UUID listingId;
	private LocalDate checkIn;
	private LocalDate checkOut;
	private int nights;
	private int rooms;
	private int guests;
	private String currency;
	private List<RoomAvailabilityDto> roomTypes;
}
