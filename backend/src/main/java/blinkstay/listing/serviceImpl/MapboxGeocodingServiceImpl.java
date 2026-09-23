package blinkstay.listing.serviceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

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
		// 1. Resolve country input to a valid ISO 3166-1 alpha-2 code (e.g., "India" ->
		// "IN")
		String countryCode = getIso2CountryCode(country);

		// 2. Query Mapbox with location and the dynamic country filter
		MapboxResponse response = webClient.get().uri(uriBuilder -> {
			var builder = uriBuilder.scheme("https").host("api.mapbox.com").path("/search/geocode/v6/forward")
					.queryParam("q", location).queryParam("limit", 1).queryParam("access_token", accessToken);

			// Add country parameter only if countryCode is valid
			if (countryCode != null && !countryCode.isBlank()) {
				builder.queryParam("country", countryCode.toUpperCase());
			}

			return builder.build();
		}).retrieve().bodyToMono(MapboxResponse.class).block();

		// 3. Handle empty response
		if (response == null || response.getFeatures() == null || response.getFeatures().isEmpty()) {
			throw new IllegalArgumentException("Unable to find coordinates for location: " + location + ", " + country);
		}

		// 4. Extract coordinates and return result
		MapboxFeature feature = response.getFeatures().get(0);
		List<BigDecimal> coordinate = feature.getGeometry().getCoordinates();

		BigDecimal longitude = coordinate.get(0);
		BigDecimal latitude = coordinate.get(1);

		String fullAddress = location + (country != null ? ", " + country : "");

		return GeocodingResult.builder().address(fullAddress).latitude(latitude).longitude(longitude).build();
	}

	private String getIso2CountryCode(String countryName) {
		if (countryName == null || countryName.trim().isEmpty()) {
			return null;
		}
		String trimmed = countryName.trim();

		// Already a 2-letter code (e.g., "US", "IN")
		if (trimmed.length() == 2) {
			return trimmed.toUpperCase();
		}

		// Match full country names (e.g., "United Stated", "India")
		for (String isoCOde : Locale.getISOCountries()) {
			Locale locale = new Locale.Builder().setRegion(isoCOde).build();
			if (locale.getDisplayCountry(Locale.ENGLISH).equalsIgnoreCase(trimmed)) {
				return isoCOde;
			}
		}

		throw new IllegalArgumentException("Invalid or unsupported country name: " + countryName);
	}

}
