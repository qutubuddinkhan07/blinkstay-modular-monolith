package blinkstay.room.dto;

import blinkstay.room.enums.RoomCategory;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class RoomResponseDto {
    private UUID roomId;

    private UUID listingId;

    private RoomCategory roomType;

    private BigDecimal price;

    private Integer totalRooms;

    private Integer maxGuests;
}
