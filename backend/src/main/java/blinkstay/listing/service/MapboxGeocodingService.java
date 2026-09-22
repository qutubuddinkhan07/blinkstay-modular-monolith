package blinkstay.listing.service;

import blinkstay.listing.dto.GeocodingResult;

public interface MapboxGeocodingService {
	GeocodingResult getCoordinates(String location, String country);
}
