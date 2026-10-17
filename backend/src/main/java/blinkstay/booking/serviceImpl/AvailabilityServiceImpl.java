package blinkstay.booking.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.booking.config.BookingProperties;
import blinkstay.booking.dto.AvailabilityResponse;
import blinkstay.booking.dto.RoomAvailabilityDto;
import blinkstay.booking.entities.Booking;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.booking.service.AvailabilityService;
import blinkstay.booking.service.BookingRules;
import blinkstay.listing.dto.ListingBookingInfo;
import blinkstay.listing.service.ListingService;
import blinkstay.room.service.RoomService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {
	private final ListingService listingService;
	private final RoomService roomService;
	private final BookingRepository bookingRepo;
	private final BookingRules rules;
	private final BookingProperties props;

	@Override
	@Transactional(readOnly = true)
	public AvailabilityResponse check(UUID listingId, LocalDate checkIn, LocalDate checkOut, int rooms, int guests) {
		int nights = rules.validateStay(checkIn, checkOut, rooms, guests);

		ListingBookingInfo listing = listingService.getBookingInfo(listingId);
		if (!listing.published()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not available");
		}

		Map<UUID, List<Booking>> byRoom = bookingRepo
				.findActiveOverlappingForListing(listingId, checkIn, checkOut, Instant.now()).stream()
				.collect(Collectors.groupingBy(Booking::getRoomId));

		List<RoomAvailabilityDto> types = roomService.getRoomsByListingId(listingId).stream().map(r -> {
			int peak = BookingRules.peakUsage(byRoom.getOrDefault(r.getRoomId(), List.of()), checkIn, checkOut);
			int free = Math.max(0, r.getTotalRooms() - peak);

			String reason = null;
			if (free < rooms) {
				reason = free == 0 ? "Sold out for these dates" : "Only " + free + " left for these dates";
			} else if (guests > rooms * r.getMaxGuests()) {
				reason = "Sleeps up to " + rooms * r.getMaxGuests() + " guests with " + rooms + " room(s)";
			}

			BigDecimal total = r.getPrice().multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(rooms))
					.setScale(2, RoundingMode.HALF_UP);

			return RoomAvailabilityDto.builder().roomId(r.getRoomId()).roomType(r.getRoomType().name())
					.pricePerNight(r.getPrice()).maxGuests(r.getMaxGuests()).totalRooms(r.getTotalRooms())
					.freeRooms(free).totalPrice(total).bookable(reason == null).reason(reason).build();
		}).sorted(Comparator.comparing(RoomAvailabilityDto::getPricePerNight)).toList();

		return AvailabilityResponse.builder().listingId(listingId).checkIn(checkIn).checkOut(checkOut).nights(nights)
				.rooms(rooms).guests(guests).currency(props.getCurrency()).roomTypes(types).build();
	}
}