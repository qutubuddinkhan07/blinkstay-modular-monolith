package blinkstay.listing.serviceImpl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import blinkstay.listing.dto.GeocodingResult;
import blinkstay.listing.dto.MapboxFeature;
import blinkstay.listing.dto.MapboxResponse;
import blinkstay.listing.service.MapboxGeocodingService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MapboxGeocodingServiceImpl implements MapboxGeocodingService {
	private final WebClient webClient;

	@Value("${mapbox.access-token}")
	private String accessToken;

	@Override
	public GeocodingResult getCoordinates(String location, String country) {
		String query = location + ", " + country;
		MapboxResponse response = webClient.get()
				.uri(uriBuilder -> uriBuilder.scheme("https").host("api.mapbox.com").path("/search/geocode/v6/forward")
						.queryParam("q", query).queryParam("country", "IN").queryParam("limit", 1)
						.queryParam("access_token", accessToken).build())
				.retrieve().bodyToMono(MapboxResponse.class).block();

		if (response == null || response.getFeatures() == null || response.getFeatures().isEmpty()) {
			throw new IllegalArgumentException("Unable to find coordinates for location: " + query);
		}

		MapboxFeature feature = response.getFeatures().get(0);

		List<BigDecimal> coordinate = feature.getGeometry().getCoordinates();

		BigDecimal longitude = coordinate.get(0);
		BigDecimal latitude = coordinate.get(1);

		return GeocodingResult.builder().address(query).latitude(latitude).longitude(longitude).build();
	}

}
