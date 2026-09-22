package blinkstay.room.dto;

import java.math.BigDecimal;

import blinkstay.room.enums.RoomCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class AddRoomDto {
	@NotNull(message = "roomType is required and must be a valid RoomCategory (e.g., SINGLE, DOUBLE, DELUXE, etc.)")
	private RoomCategory roomType;

	@NotNull(message = "Price is required")
	private BigDecimal price;

	@NotNull(message = "Total rooms count is required")
	@Min(value = 1, message = "Total rooms must be at least 1")
	private Integer totalRooms;

	@NotNull(message = "Available rooms count is required")
	@Min(value = 0, message = "Available rooms cannot be negative")
	private Integer availableRooms;
}
