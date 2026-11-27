package com.blinkstay.booking.serviceImpl;

import com.blinkstay.booking.config.BookingProperties;
import com.blinkstay.booking.dto.CreateBookingDto;
import com.blinkstay.booking.entities.Booking;
import com.blinkstay.booking.enums.BookingStatus;
import com.blinkstay.booking.mapper.BookingMapper;
import com.blinkstay.booking.repository.BookingRepository;
import com.blinkstay.booking.service.BookingRules;
import com.blinkstay.listing.dto.ListingBookingInfo;
import com.blinkstay.listing.service.ListingService;
import com.blinkstay.room.dto.RoomResponseDto;
import com.blinkstay.room.enums.RoomCategory;
import com.blinkstay.room.service.RoomService;
import com.blinkstay.support.TestDates;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.blinkstay.support.builders.BookingBuilder.aBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Creating a booking (Q47, Q113 to Q118): validate, lock the room FIRST, check listing, capacity,
 * the 2-unpaid-holds limit and free rooms per night, then save a PENDING_PAYMENT booking with a
 * 15-minute hold and a price snapshot.
 * <p>
 * Real BookingRules and BookingProperties (pure, already tested); mocks for the repository,
 * the room and listing modules and the mapper.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private BookingMapper mapper;
    @Mock
    private RoomService roomService;
    @Mock
    private ListingService listingService;

    private BookingServiceImpl service;
    private LocalDate today;

    private final UUID guestId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        BookingProperties props = new BookingProperties(); // hold 15 min, max 2 unpaid, INR
        service = new BookingServiceImpl(bookingRepo, new BookingRules(props), props, mapper, roomService,
                listingService, new SecureRandom());
        today = TestDates.today();
    }

    // ---------------- create: happy path ----------------

    @Nested
    @DisplayName("create: success")
    class CreateSuccess {

        @Test
        void freeRooms_savesPendingBookingWithHoldAndPriceSnapshot() {
            // Arrange: 3 double rooms at 2000.00; guest wants 2 rooms for 2 nights, 3 guests
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 2, 3);
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(true, hostId);
            arrangeNoHolds();
            arrangeOverlapping(List.of());
            arrangeReferenceFree();
            ArgumentCaptor<Booking> saved = arrangeSave();

            // Act
            Instant before = Instant.now();
            service.create(guestId, dto);

            // Assert
            Booking b = saved.getValue();
            assertThat(b.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
            assertThat(b.getGuestId()).isEqualTo(guestId);
            assertThat(b.getHostId()).isEqualTo(hostId);
            assertThat(b.getListingId()).isEqualTo(listingId);
            assertThat(b.getRoomId()).isEqualTo(roomId);
            assertThat(b.getNights()).isEqualTo(2);
            assertThat(b.getRoomsBooked()).isEqualTo(2);
            assertThat(b.getPricePerNight()).isEqualByComparingTo("2000.00");
            assertThat(b.getTotalAmount()).isEqualByComparingTo("8000.00"); // 2000 x 2 nights x 2 rooms
            assertThat(b.getCurrency()).isEqualTo("INR");
            assertThat(b.getHoldExpiresAt()).isBetween(before.plus(Duration.ofMinutes(15)),
                    Instant.now().plus(Duration.ofMinutes(15)));
            assertThat(b.getListingTitle()).isEqualTo("Sea view villa"); // snapshot (Q50)
            assertThat(b.getRoomType()).isEqualTo(RoomCategory.values()[0].name());
            assertThat(b.getReference()).matches("BS-[A-HJ-NP-Z2-9]{8}"); // no 0, O, 1, I
            verify(mapper).toDto(eq(b), any(Instant.class));
        }

        @Test
        void roomLockIsTheFirstStep_beforeAnyOtherRead() {
            // Q113: parallel bookings of this room type queue at the lock, so the checks after it
            // always see what the previous booking committed
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 1, 1);
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(true, hostId);
            arrangeNoHolds();
            arrangeOverlapping(List.of());
            arrangeReferenceFree();
            arrangeSave();

            service.create(guestId, dto);

            InOrder order = inOrder(roomService, listingService, bookingRepo);
            order.verify(roomService).lockRoom(roomId);
            order.verify(listingService).getBookingInfo(listingId);
            order.verify(bookingRepo).countPendingHolds(eq(guestId), any(Instant.class));
            order.verify(bookingRepo).findActiveOverlapping(eq(roomId), any(), any(), any(Instant.class));
            order.verify(bookingRepo).save(any(Booking.class));
        }

        @Test
        void exactlyEnoughRoomsLeft_isAccepted() {
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 2, 2);
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(true, hostId);
            arrangeNoHolds();
            arrangeOverlapping(List.of(existing(today.plusDays(10), 2, 1))); // 1 room used -> 2 free
            arrangeReferenceFree();
            ArgumentCaptor<Booking> saved = arrangeSave();

            service.create(guestId, dto);

            assertThat(saved.getValue().getRoomsBooked()).isEqualTo(2);
        }

        @Test
        void total_isPriceTimesNightsTimesRooms_toTwoDecimals() {
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(13), 1, 1); // 3 nights
            arrangeRoom(3, 2, "1500.50");
            arrangeListing(true, hostId);
            arrangeNoHolds();
            arrangeOverlapping(List.of());
            arrangeReferenceFree();
            ArgumentCaptor<Booking> saved = arrangeSave();

            service.create(guestId, dto);

            assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("4501.50");
        }

        @Test
        void referenceAlreadyTaken_triesANewOne() {
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 1, 1);
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(true, hostId);
            arrangeNoHolds();
            arrangeOverlapping(List.of());
            when(bookingRepo.existsByReference(anyString())).thenReturn(true, false); // 1st taken, 2nd free
            ArgumentCaptor<Booking> saved = arrangeSave();

            service.create(guestId, dto);

            verify(bookingRepo, times(2)).existsByReference(anyString());
            assertThat(saved.getValue().getReference()).startsWith("BS-");
        }
    }

    // ---------------- create: refusals ----------------

    @Nested
    @DisplayName("create: refused")
    class CreateRefused {

        @Test
        void invalidStay_isRejectedBeforeTheRoomIsLocked() {
            CreateBookingDto dto = request(today.plusDays(1), today.plusDays(32), 1, 1); // 31 nights

            assertError(() -> service.create(guestId, dto), HttpStatus.BAD_REQUEST, "The maximum stay is 30 nights");
            verifyNoInteractions(roomService, listingService, bookingRepo);
        }

        @Test
        void unpublishedListing_isNotFound() {
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 1, 1);
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(false, hostId);

            assertError(() -> service.create(guestId, dto), HttpStatus.NOT_FOUND, "Listing not available");
            verify(bookingRepo, never()).save(any());
        }

        @Test
        void hostBookingOwnListing_isBadRequest() {
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 1, 1);
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(true, guestId); // the "guest" is the manager

            assertError(() -> service.create(guestId, dto), HttpStatus.BAD_REQUEST, "You cannot book your own listing");
        }

        @Test
        void moreGuestsThanTheRoomsSleep_isBadRequest() {
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 1, 3); // 1 room sleeps 2
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(true, hostId);

            assertError(() -> service.create(guestId, dto), HttpStatus.BAD_REQUEST, "These rooms sleep up to 2 guests");
        }

        @Test
        void twoUnpaidHoldsAlready_isConflict() {
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 1, 1);
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(true, hostId);
            when(bookingRepo.countPendingHolds(eq(guestId), any(Instant.class))).thenReturn(2L);

            assertError(() -> service.create(guestId, dto), HttpStatus.CONFLICT,
                    "You already have 2 unpaid bookings. Pay for them or wait until they expire.");
            verify(bookingRepo, never()).findActiveOverlapping(any(), any(), any(), any());
        }

        @Test
        void allRoomsTakenOnSomeNight_isSoldOut() {
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 1, 1);
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(true, hostId);
            arrangeNoHolds();
            arrangeOverlapping(List.of(existing(today.plusDays(11), 1, 3))); // night of the 11th: 3 of 3 used

            assertError(() -> service.create(guestId, dto), HttpStatus.CONFLICT, "Sold out for these dates");
            verify(bookingRepo, never()).save(any());
        }

        @Test
        void fewerRoomsLeftThanRequested_saysHowManyAreLeft() {
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 2, 2);
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(true, hostId);
            arrangeNoHolds();
            arrangeOverlapping(List.of(existing(today.plusDays(10), 2, 2))); // 2 used -> 1 free

            assertError(() -> service.create(guestId, dto), HttpStatus.CONFLICT, "Only 1 room(s) left for these dates");
        }

        @Test
        void noFreeReferenceAfterFiveTries_isServerError() {
            CreateBookingDto dto = request(today.plusDays(10), today.plusDays(12), 1, 1);
            arrangeRoom(3, 2, "2000.00");
            arrangeListing(true, hostId);
            arrangeNoHolds();
            arrangeOverlapping(List.of());
            when(bookingRepo.existsByReference(anyString())).thenReturn(true);

            assertError(() -> service.create(guestId, dto), HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not create a booking reference. Please try again.");
            verify(bookingRepo, times(5)).existsByReference(anyString());
        }
    }

    // ---------------- reads and timers ----------------

    @Nested
    @DisplayName("getMine and myBookings")
    class Reads {

        @Test
        void getMine_ownBooking_isMapped() {
            Booking b = aBooking().withId().guest(guestId).build();
            when(bookingRepo.findById(b.getId())).thenReturn(Optional.of(b));

            service.getMine(guestId, b.getId());

            verify(mapper).toDto(eq(b), any(Instant.class));
        }

        @Test
        void getMine_someoneElsesBooking_isNotFound() {
            Booking b = aBooking().withId().build();
            when(bookingRepo.findById(b.getId())).thenReturn(Optional.of(b));

            assertError(() -> service.getMine(guestId, b.getId()), HttpStatus.NOT_FOUND, "Booking not found");
        }

        @ParameterizedTest(name = "page {0}, size {1} -> page {2}, size {3}")
        @CsvSource({"-1, 10, 0, 10", "0, 0, 0, 1", "2, 100, 2, 50", "1, 20, 1, 20"})
        void pageAndSize_areClamped(int page, int size, int expectedPage, int expectedSize) {
            Pageable used = callMyBookings(null, null, page, size);

            assertThat(used.getPageNumber()).isEqualTo(expectedPage);
            assertThat(used.getPageSize()).isEqualTo(expectedSize);
        }

        @Test
        void unknownSortField_fallsBackToCreatedAt() {
            Pageable used = callMyBookings("password", "asc", 0, 10); // whitelist: never sort by any column

            assertThat(used.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
        }

        @Test
        void sortByCheckIn_addsCreatedAtAsTieBreaker() {
            Pageable used = callMyBookings("checkIn", "asc", 0, 10);

            assertThat(used.getSort()).isEqualTo(
                    Sort.by(Sort.Direction.ASC, "checkIn").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        @SuppressWarnings("unchecked")
        private Pageable callMyBookings(String sort, String direction, int page, int size) {
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            when(bookingRepo.findAll(any(Specification.class), pageable.capture()))
                    .thenAnswer(inv -> new PageImpl<Booking>(List.of(), inv.getArgument(1), 0));

            service.myBookings(guestId, List.of(), sort, direction, page, size);

            return pageable.getValue();
        }
    }

    @Nested
    @DisplayName("timers")
    class Timers {

        @Test
        void expireStaleHolds_returnsHowManyWereExpired() {
            when(bookingRepo.expireHolds(any(Instant.class))).thenReturn(3);

            assertThat(service.expireStaleHolds()).isEqualTo(3);
        }

        @Test
        void completeFinishedStays_usesTodayInTheHotelTimeZone() {
            when(bookingRepo.completeFinished(eq(today), any(Instant.class))).thenReturn(2);

            assertThat(service.completeFinishedStays()).isEqualTo(2);
        }
    }

    // ---------------- helpers (each one stubs ONE step of create) ----------------

    private CreateBookingDto request(LocalDate checkIn, LocalDate checkOut, int rooms, int guests) {
        CreateBookingDto dto = new CreateBookingDto();
        dto.setRoomId(roomId);
        dto.setCheckIn(checkIn);
        dto.setCheckOut(checkOut);
        dto.setRooms(rooms);
        dto.setGuests(guests);
        return dto;
    }

    private void arrangeRoom(int totalRooms, int maxGuests, String price) {
        RoomResponseDto room = RoomResponseDto.builder().roomId(roomId).listingId(listingId)
                .roomType(RoomCategory.values()[0]).price(new BigDecimal(price))
                .totalRooms(totalRooms).maxGuests(maxGuests).build();
        when(roomService.lockRoom(roomId)).thenReturn(room);
    }

    private void arrangeListing(boolean published, UUID managerId) {
        when(listingService.getBookingInfo(listingId)).thenReturn(new ListingBookingInfo(listingId, managerId,
                "Sea view villa", "Goa", "https://img.example/cover.jpg", published));
    }

    private void arrangeNoHolds() {
        when(bookingRepo.countPendingHolds(eq(guestId), any(Instant.class))).thenReturn(0L);
    }

    private void arrangeOverlapping(List<Booking> bookings) {
        when(bookingRepo.findActiveOverlapping(eq(roomId), any(), any(), any(Instant.class))).thenReturn(bookings);
    }

    private void arrangeReferenceFree() {
        when(bookingRepo.existsByReference(anyString())).thenReturn(false);
    }

    /**
     * save() returns the booking it was given, and the captor remembers it for the asserts.
     */
    private ArgumentCaptor<Booking> arrangeSave() {
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        return captor;
    }

    private Booking existing(LocalDate checkIn, int nights, int rooms) {
        return aBooking().room(roomId).checkIn(checkIn).nights(nights).rooms(rooms).build();
    }

    private static void assertError(ThrowingCallable call, HttpStatus status, String reason) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, e -> {
            assertThat(e.getStatusCode()).isEqualTo(status);
            assertThat(e.getReason()).isEqualTo(reason);
        });
    }
}