package blinkstay.common.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PastStay(UUID guestId, UUID hostId, UUID listingId, UUID roomId, String roomType,
                       BigDecimal pricePerNight, LocalDate checkIn, LocalDate checkOut, int guests,
                       String listingTitle, String listingLocation, String coverImageUrl, Instant bookedAt) {
}