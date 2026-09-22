package blinkstay.listing.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapboxGeometry {

	private List<BigDecimal> coordinates;
}
