package blinkstay.room.dto;

import java.math.BigDecimal;
import java.util.UUID;

import blinkstay.room.enums.RoomCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class RoomResponseDto {
	private UUID listingId;

	private RoomCategory roomType;

	private BigDecimal price;

	private Integer totalRooms;

	private Integer availableRooms;
}
