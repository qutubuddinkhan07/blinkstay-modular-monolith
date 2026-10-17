package blinkstay.booking.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookingDto {
	@NotNull(message = "Room is required")
	private UUID roomId;
	@NotNull(message = "Check-in date is required")
	private LocalDate checkIn;
	@NotNull(message = "Check-out date is required")
	private LocalDate checkOut;
	@NotNull(message = "Number of rooms is required")
	@Min(value = 1, message = "At least 1 room")
	@Max(value = 10, message = "At most 10 rooms")
	private Integer rooms;
	@NotNull(message = "Number of guests is required")
	@Min(value = 1, message = "At least 1 guest")
	@Max(value = 50, message = "At most 50 guests")
	private Integer guests;
}
