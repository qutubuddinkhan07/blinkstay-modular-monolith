package blinkstay.seed.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeedGeometry {

	private String type;

	private BigDecimal[] coordinates;
}