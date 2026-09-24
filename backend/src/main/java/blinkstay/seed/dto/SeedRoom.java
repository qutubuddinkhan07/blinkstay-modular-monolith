package blinkstay.seed.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeedRoom {

	private String type;

	private BigDecimal priceAdjustment;

	private Integer totalRooms;

	private Integer availableRooms;
}