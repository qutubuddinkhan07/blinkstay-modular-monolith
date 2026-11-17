package blinkstay.booking.service;

import blinkstay.booking.dto.HostCalendarDto;

import java.util.UUID;

public interface HostCalendarService {
    /**
     * month: "2026-12", or null/blank for the current month.
     */
    HostCalendarDto calendar(UUID hostId, UUID listingId, String month);
}