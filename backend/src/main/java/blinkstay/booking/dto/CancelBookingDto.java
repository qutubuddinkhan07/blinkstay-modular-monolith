package blinkstay.booking.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelBookingDto {
    @Size(max = 500, message = "The reason can be at most 500 characters")
    private String reason;
}
