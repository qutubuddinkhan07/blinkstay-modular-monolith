package blinkstay.booking.serviceImpl;

import blinkstay.booking.dto.CalendarBookingDto;
import blinkstay.booking.dto.CalendarDayDto;
import blinkstay.booking.dto.HostCalendarDto;
import blinkstay.booking.dto.RoomCalendarDto;
import blinkstay.booking.entities.Booking;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.booking.service.BookingRules;
import blinkstay.booking.service.HostCalendarService;
import blinkstay.common.directory.UserContact;
import blinkstay.common.directory.UserDirectory;
import blinkstay.listing.service.ListingService;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HostCalendarServiceImpl implements HostCalendarService {
    private static final int MAX_MONTHS_AWAY = 24;

    private final BookingRepository bookingRepo;
    private final BookingRules rules;
    private final ListingService listingService;
    private final RoomService roomService;
    private final UserDirectory userDirectory;

    @Override
    @Transactional(readOnly = true)
    public HostCalendarDto calendar(UUID hostId, UUID listingId, String month) {
        // 404 if the listing does not exist, 403 if it belongs to another host
        listingService.checkWhetherSameManager(hostId, listingId);

        LocalDate today = rules.today();
        YearMonth ym = parseMonth(month, today);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.plusMonths(1).atDay(1); // exclusive
        int days = ym.lengthOfMonth();

        List<RoomResponseDto> rooms = roomService.getRoomsByListingId(listingId);
        List<Booking> bookings = bookingRepo.findForCalendar(listingId, from, to, Instant.now());

        // per room: rooms used on each night of the month (index 0 = the 1st)
        Map<UUID, int[]> booked = new HashMap<>();
        Map<UUID, int[]> held = new HashMap<>();

        for (Booking b : bookings) {
            Map<UUID, int[]> bucket = b.getStatus() == BookingStatus.PENDING_PAYMENT ? held : booked;
            int[] nights = bucket.computeIfAbsent(b.getRoomId(), k -> new int[days]);

            // a night is the date you sleep there: check-in counts, check-out does not
            LocalDate start = b.getCheckIn().isBefore(from) ? from : b.getCheckIn();
            LocalDate end = b.getCheckOut().isAfter(to) ? to : b.getCheckOut();
            for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
                nights[d.getDayOfMonth() - 1] += b.getRoomsBooked();
            }
        }

        List<RoomCalendarDto> roomDtos = new ArrayList<>();
        for (RoomResponseDto r : rooms) {
            int total = r.getTotalRooms() == null ? 0 : r.getTotalRooms();
            int[] bk = booked.getOrDefault(r.getRoomId(), new int[days]);
            int[] hd = held.getOrDefault(r.getRoomId(), new int[days]);

            List<CalendarDayDto> dayDtos = new ArrayList<>(days);
            for (int i = 0; i < days; i++) {
                LocalDate d = from.plusDays(i);
                dayDtos.add(new CalendarDayDto(d, bk[i], hd[i], Math.max(0, total - bk[i] - hd[i]), d.isBefore(today)));
            }
            roomDtos.add(new RoomCalendarDto(r.getRoomId(), r.getRoomType() == null ? null : r.getRoomType().name(),
                    total, dayDtos));
        }

        // one name lookup per distinct guest
        Map<UUID, String> names = new HashMap<>();
        List<CalendarBookingDto> bookingDtos = bookings.stream()
                .map(b -> new CalendarBookingDto(b.getId(), b.getReference(), b.getRoomId(), b.getRoomType(),
                        names.computeIfAbsent(b.getGuestId(),
                                id -> userDirectory.findContact(id).map(UserContact::username).orElse("Guest")),
                        b.getCheckIn(), b.getCheckOut(), b.getRoomsBooked(), b.getGuests(), b.getStatus()))
                .toList();

        return new HostCalendarDto(listingId, ym.toString(), today, roomDtos, bookingDtos);
    }

    private YearMonth parseMonth(String month, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        if (month == null || month.isBlank()) {
            return current;
        }
        YearMonth ym;
        try {
            ym = YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must look like 2026-12");
        }
        if (Math.abs(ChronoUnit.MONTHS.between(current, ym)) > MAX_MONTHS_AWAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The calendar shows up to " + MAX_MONTHS_AWAY + " months back or ahead");
        }
        return ym;
    }
}