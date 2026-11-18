package blinkstay.booking.serviceImpl;

import blinkstay.booking.entities.Booking;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.common.seed.PastStay;
import blinkstay.common.seed.PastStayWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Writes finished stays for the demo seed. Completed bookings never count against availability.
 */
@Service
@RequiredArgsConstructor
public class PastStayWriterImpl implements PastStayWriter {

    private static final String REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final BookingRepository bookingRepo;

    @Override
    @Transactional
    public UUID createCompletedStay(PastStay s) {
        int nights = (int) ChronoUnit.DAYS.between(s.checkIn(), s.checkOut());
        Booking booking = bookingRepo.save(Booking.builder()
                .reference(newReference())
                .guestId(s.guestId()).hostId(s.hostId()).listingId(s.listingId()).roomId(s.roomId())
                .checkIn(s.checkIn()).checkOut(s.checkOut()).nights(nights)
                .roomsBooked(1).guests(s.guests())
                .status(BookingStatus.COMPLETED)
                .pricePerNight(s.pricePerNight())
                .totalAmount(s.pricePerNight().multiply(BigDecimal.valueOf(nights)))
                .currency("INR")
                .confirmedAt(s.bookedAt())
                .listingTitle(s.listingTitle()).listingLocation(s.listingLocation())
                .coverImageUrl(s.coverImageUrl())
                .roomType(s.roomType())
                .build());

        // @CreationTimestamp sets "now"; move it back to when the stay was booked
        bookingRepo.backdate(booking.getId(), s.bookedAt());
        return booking.getId();
    }

    // "SD" marks seeded bookings; 10 characters fits the 12-character column
    private String newReference() {
        String ref;
        do {
            StringBuilder sb = new StringBuilder("SD");
            for (int i = 0; i < 8; i++) {
                sb.append(REF_CHARS.charAt(random.nextInt(REF_CHARS.length())));
            }
            ref = sb.toString();
        } while (bookingRepo.existsByReference(ref));
        return ref;
    }
}