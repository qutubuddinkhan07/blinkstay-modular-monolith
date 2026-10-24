package blinkstay.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// the seed file may still carry old fields such as availableRooms; ignore them
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class SeedRoom {
    private String type;
    private BigDecimal priceAdjustment;
    private Integer totalRooms;
}