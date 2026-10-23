package blinkstay.booking.serviceImpl;

import blinkstay.booking.dto.BookingFilter;
import blinkstay.booking.dto.StaffBookingDto;
import blinkstay.booking.entities.Booking;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.mapper.BookingMapper;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.booking.repository.BookingSpecifications;
import blinkstay.booking.service.BookingRules;
import blinkstay.booking.service.StaffBookingService;
import blinkstay.common.directory.UserContact;
import blinkstay.common.directory.UserDirectory;
import blinkstay.common.dto.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StaffBookingServiceImpl implements StaffBookingService {
    private final BookingRepository bookingRepo;
    private final UserDirectory userDirectory;
    private final BookingRules rules;

    @Override
    @Transactional(readOnly = true)
    public PageResult<StaffBookingDto> hostBookings(UUID hostId, BookingFilter f) {
        Page<Booking> page = bookingRepo.findAll(
                BookingSpecifications.forHost(hostId, f.statuses(), f.listingId(), f.from(), f.to()), pageable(f));
        return toPage(page, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StaffBookingDto> adminBookings(String q, UUID guestId, UUID hostId, BookingFilter f) {
        Page<Booking> page = bookingRepo.findAll(
                BookingSpecifications.forAdmin(q, guestId, hostId, f.statuses(), f.listingId(), f.from(), f.to()),
                pageable(f));
        return toPage(page, true);
    }

    private PageResult<StaffBookingDto> toPage(Page<Booking> page, boolean admin) {
        // one lookup per distinct person on this page (at most 50 rows)
        Set<UUID> ids = new HashSet<>();
        for (Booking b : page.getContent()) {
            ids.add(b.getGuestId());
            if (admin) ids.add(b.getHostId());
        }
        Map<UUID, UserContact> people = new HashMap<>();
        for (UUID id : ids) {
            userDirectory.findContact(id).ifPresent(c -> people.put(id, c));
        }

        LocalDate today = rules.today();
        List<StaffBookingDto> rows = page.getContent().stream().map(b -> {
            UserContact guest = people.get(b.getGuestId());
            UserContact host = people.get(b.getHostId());
            boolean cancellable = (b.getStatus() == BookingStatus.CONFIRMED && b.getCheckOut().isAfter(today))
                    || (admin && b.getStatus() == BookingStatus.PENDING_PAYMENT);
            return StaffBookingDto.builder().id(b.getId()).reference(b.getReference()).listingId(b.getListingId())
                    .listingTitle(b.getListingTitle()).roomType(b.getRoomType()).checkIn(b.getCheckIn())
                    .checkOut(b.getCheckOut()).nights(b.getNights()).rooms(b.getRoomsBooked()).guests(b.getGuests())
                    .totalAmount(b.getTotalAmount()).currency(b.getCurrency()).status(b.getStatus())
                    .createdAt(b.getCreatedAt()).confirmedAt(b.getConfirmedAt()).cancelledAt(b.getCancelledAt())
                    .cancelledByRole(BookingMapper.cancelledByRole(b)).cancellationReason(b.getCancellationReason())
                    .refundAmount(b.getRefundAmount()).cancellable(cancellable)
                    .guestName(guest != null ? guest.username() : "Deleted user")
                    .guestEmail(admin && guest != null ? guest.email() : null)
                    .hostName(admin ? (host != null ? host.username() : "Deleted user") : null).build();
        }).toList();

        return new PageResult<>(rows, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }

    private static Pageable pageable(BookingFilter f) {
        String field = "checkIn".equalsIgnoreCase(f.sort()) ? "checkIn" : "createdAt"; // whitelist
        Sort.Direction dir = "asc".equalsIgnoreCase(f.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort order = Sort.by(dir, field);
        if (!field.equals("createdAt")) order = order.and(Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageRequest.of(Math.max(f.page(), 0), Math.min(Math.max(f.size(), 1), 50), order);
    }
}
