package blinkstay.listing.service;

import blinkstay.listing.dtos.GeocodingResult;

public interface MapboxGeocodingService {
	GeocodingResult getCoordinates(String location, String country);
}
