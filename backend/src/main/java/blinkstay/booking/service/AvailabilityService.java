package blinkstay.booking.service;

import java.time.LocalDate;
import java.util.UUID;

import blinkstay.booking.dto.AvailabilityResponse;

public interface AvailabilityService {
	AvailabilityResponse check(UUID listingId, LocalDate checkIn, LocalDate checkOut, int rooms, int guests);
}
