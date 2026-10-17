package blinkstay.listing.service;

import java.math.BigDecimal;

import blinkstay.listing.dto.GeocodingResult;

public interface MapboxGeocodingService {
	GeocodingResult getCoordinates(String location, String country);

	String reverseGeocode(BigDecimal latitude, BigDecimal longitude); // null when nothing is found
}
