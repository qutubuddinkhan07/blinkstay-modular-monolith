package blinkstay.booking.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTonightDto {
    private UUID roomId;
    private String roomType;
    private int totalRooms;
    private int freeTonight; // rooms of this type nobody has for the night that starts today
}
