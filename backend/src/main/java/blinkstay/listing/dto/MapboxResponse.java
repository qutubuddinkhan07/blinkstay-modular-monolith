package blinkstay.listing.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapboxResponse {
	private List<MapboxFeature> features;
}
