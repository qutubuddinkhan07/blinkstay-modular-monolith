package blinkstay.listing.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapboxFeature {
	private MapboxGeometry geometry;
	private MapboxProperties properties;
}
