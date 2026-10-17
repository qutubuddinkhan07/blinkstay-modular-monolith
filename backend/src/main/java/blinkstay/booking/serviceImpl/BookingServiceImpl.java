package blinkstay.booking.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.booking.config.BookingProperties;
import blinkstay.booking.dto.BookingResponseDto;
import blinkstay.booking.dto.CreateBookingDto;
import blinkstay.booking.entities.Booking;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.mapper.BookingMapper;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.booking.repository.BookingSpecifications;
import blinkstay.booking.service.BookingRules;
import blinkstay.booking.service.BookingService;
import blinkstay.common.dto.PageResult;
import blinkstay.listing.dto.ListingBookingInfo;
import blinkstay.listing.service.ListingService;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {
	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I

	private final BookingRepository bookingRepo;
	private final BookingRules rules;
	private final BookingProperties props;
	private final BookingMapper mapper;
	private final RoomService roomService;
	private final ListingService listingService;

	@Qualifier("random")
	private final SecureRandom random;

	@Override
	@Transactional
	public BookingResponseDto create(UUID guestId, CreateBookingDto dto) {
		int nights = rules.validateStay(dto.getCheckIn(), dto.getCheckOut(), dto.getRooms(), dto.getGuests());

		// RULE: the room lock must be the FIRST database statement of this transaction.
		// Parallel bookings of
		// this room type queue up here, and everything read after the lock sees what
		// the previous one committed.
		RoomResponseDto room = roomService.lockRoom(dto.getRoomId());

		ListingBookingInfo listing = listingService.getBookingInfo(room.getListingId());
		if (!listing.published()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not available");
		}
		if (listing.managerId().equals(guestId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot book your own listing");
		}
		if (dto.getGuests() > dto.getRooms() * room.getMaxGuests()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"These rooms sleep up to " + dto.getRooms() * room.getMaxGuests() + " guests");
		}

		Instant now = Instant.now();
		if (bookingRepo.countPendingHolds(guestId, now) >= props.getMaxPendingPerGuest()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have " + props.getMaxPendingPerGuest()
					+ " unpaid bookings. Pay for them or wait until they expire.");
		}

		List<Booking> overlapping = bookingRepo.findActiveOverlapping(room.getRoomId(), dto.getCheckIn(),
				dto.getCheckOut(), now);
		int free = room.getTotalRooms() - BookingRules.peakUsage(overlapping, dto.getCheckIn(), dto.getCheckOut());
		if (free < dto.getRooms()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					free <= 0 ? "Sold out for these dates" : "Only " + free + " room(s) left for these dates");
		}

		BigDecimal total = room.getPrice().multiply(BigDecimal.valueOf(nights))
				.multiply(BigDecimal.valueOf(dto.getRooms())).setScale(2, RoundingMode.HALF_UP);

		Booking booking = Booking.builder().reference(newReference()).guestId(guestId).hostId(listing.managerId())
				.listingId(room.getListingId()).roomId(room.getRoomId()).checkIn(dto.getCheckIn())
				.checkOut(dto.getCheckOut()).nights(nights).roomsBooked(dto.getRooms()).guests(dto.getGuests())
				.status(BookingStatus.PENDING_PAYMENT).pricePerNight(room.getPrice()).totalAmount(total)
				.currency(props.getCurrency()).holdExpiresAt(now.plus(Duration.ofMinutes(props.getHoldMinutes())))
				.listingTitle(listing.title()).listingLocation(listing.location())
				.coverImageUrl(listing.coverImageUrl()).roomType(room.getRoomType().name()).build();

		Booking saved = bookingRepo.save(booking);
		log.info("Booking {} created: guest {}, room {}, {} to {}", saved.getReference(), guestId, room.getRoomId(),
				dto.getCheckIn(), dto.getCheckOut());
		return mapper.toDto(saved, now);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<BookingResponseDto> myBookings(UUID guestId, List<BookingStatus> statuses, String sort,
			String direction, int page, int size) {
		String field = "checkIn".equalsIgnoreCase(sort) ? "checkIn" : "createdAt"; // whitelist
		Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
		Sort order = Sort.by(dir, field);
		if (!field.equals("createdAt")) {
			order = order.and(Sort.by(Sort.Direction.DESC, "createdAt"));
		}
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), order);

		Page<Booking> result = bookingRepo.findAll(BookingSpecifications.forGuest(guestId, statuses), pageable);
		Instant now = Instant.now();
		return new PageResult<>(result.getContent().stream().map(b -> mapper.toDto(b, now)).toList(),
				result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
	}

	@Override
	@Transactional(readOnly = true)
	public BookingResponseDto getMine(UUID guestId, UUID bookingId) {
		Booking b = bookingRepo.findById(bookingId).filter(x -> x.getGuestId().equals(guestId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
		return mapper.toDto(b, Instant.now());
	}

	@Override
	@Transactional
	public int expireStaleHolds() {
		return bookingRepo.expireHolds(Instant.now());
	}

	private String newReference() {
		for (int attempt = 0; attempt < 5; attempt++) {
			StringBuilder sb = new StringBuilder("BS-");
			for (int i = 0; i < 8; i++) {
				sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
			}
			if (!bookingRepo.existsByReference(sb.toString())) {
				return sb.toString();
			}
		}
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
				"Could not create a booking reference. Please try again.");
	}
}
