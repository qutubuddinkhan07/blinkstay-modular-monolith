package blinkstay.booking.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TonightAvailabilityResponse {
    private UUID listingId;
    private LocalDate date; // today in the hotel's time zone
    private List<RoomTonightDto> rooms;
}
