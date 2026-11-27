package com.blinkstay.booking.service;

import com.blinkstay.booking.dto.AvailabilityResponse;
import com.blinkstay.booking.dto.TonightAvailabilityResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface AvailabilityService {
    AvailabilityResponse check(UUID listingId, LocalDate checkIn, LocalDate checkOut, int rooms, int guests);
	
    TonightAvailabilityResponse tonight(UUID listingId);
}
