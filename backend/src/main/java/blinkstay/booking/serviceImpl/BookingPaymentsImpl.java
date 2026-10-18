package blinkstay.booking.serviceImpl;

import blinkstay.booking.entities.Booking;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.booking.service.BookingRules;
import blinkstay.common.booking.BookingConfirmedEvent;
import blinkstay.common.booking.BookingPayments;
import blinkstay.common.booking.PayableBooking;
import blinkstay.common.booking.PaymentConfirmation;
import blinkstay.listing.service.ListingService;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingPaymentsImpl implements BookingPayments {
    private final BookingRepository bookingRepo;
    private final BookingRules rules;
    private final RoomService roomService;
    private final ListingService listingService;
    private final ApplicationEventPublisher publisher;

    @Override
    public PayableBooking getPayableBooking(UUID guestId, UUID bookingId) {
        Booking b = bookingRepo.findById(bookingId).filter(x -> x.getGuestId().equals(guestId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking is already paid");
        }

        boolean payable = b.getStatus() == BookingStatus.PENDING_PAYMENT && b.getHoldExpiresAt() != null && b.getHoldExpiresAt().isAfter(Instant.now());

        if (!payable) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking can no longer be paid. Its hold has expired or it was cancelled. Please book again.");
        }

        return new PayableBooking(b.getId(), b.getReference(), b.getTotalAmount(), b.getCurrency(), b.getHoldExpiresAt());
    }

    @Override
    @Transactional
    public PaymentConfirmation confirmPayment(UUID bookingId, UUID paymentId, BigDecimal paidAmount, String currency) {
        Booking b = bookingRepo.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        // the amount is checked against OUR record, never trusted from the payment side
        if (paidAmount == null || paidAmount.compareTo(b.getTotalAmount()) != 0 || currency == null || !b.getCurrency().equalsIgnoreCase(currency)) {
            return PaymentConfirmation.MISMATCH;
        }

        switch (b.getStatus()) {
            case CONFIRMED, COMPLETED -> {
                return paymentId.equals(b.getPaymentId()) ? PaymentConfirmation.ALREADY_CONFIRMED : PaymentConfirmation.DUPLICATE;
            }
            case CANCELLED -> {
                return PaymentConfirmation.UNAVAILABLE;
            }
            default -> { /* PENDING_PAYMENT or EXPIRED: continue */}
        }

        Instant now = Instant.now();
        boolean holdValid = b.getStatus() == BookingStatus.PENDING_PAYMENT && b.getHoldExpiresAt() != null && b.getHoldExpiresAt().isAfter(now);

        if (!holdValid && !canRevive(b, now)) {
            return PaymentConfirmation.UNAVAILABLE;
        }

        b.setStatus(BookingStatus.CONFIRMED);
        b.setConfirmedAt(now);
        b.setPaymentId(paymentId);
        bookingRepo.save(b);

        publisher.publishEvent(new BookingConfirmedEvent(b.getId(), b.getReference(), b.getGuestId(), b.getHostId(), b.getListingId(), b.getListingTitle(), b.getRoomType(), b.getCheckIn(), b.getCheckOut(), b.getNights(), b.getRoomsBooked(), b.getGuests(), b.getTotalAmount(), b.getCurrency()));

        log.info("Booking {} confirmed by payment {}", b.getReference(), paymentId);

        return PaymentConfirmation.CONFIRMED;
    }

    /**
     * The hold ran out before the payment arrived: revive the booking only if the rooms are still free.
     */
    private boolean canRevive(Booking b, Instant now) {
        if (b.getCheckIn().isBefore(rules.today())) {
            return false;
        }

        try {
            if (!listingService.getBookingInfo(b.getListingId()).published()) {
                return false;
            }

            RoomResponseDto room = roomService.lockRoom(b.getRoomId());
            List<Booking> overlapping = bookingRepo.findActiveOverlapping(b.getRoomId(), b.getCheckIn(), b.getCheckOut(), now);
            int free = room.getTotalRooms() - BookingRules.peakUsage(overlapping, b.getCheckIn(), b.getCheckOut());
            return free >= b.getRoomsBooked();
        } catch (ResponseStatusException e) {
            return false; // listing or room was deleted meanwhile
        }
    }
}
