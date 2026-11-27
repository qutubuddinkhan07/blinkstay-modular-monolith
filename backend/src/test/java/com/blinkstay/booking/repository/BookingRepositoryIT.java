package com.blinkstay.booking.repository;

import com.blinkstay.booking.entities.Booking;
import com.blinkstay.booking.enums.BookingStatus;
import com.blinkstay.support.RepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.blinkstay.booking.enums.BookingStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
@DisplayName("BookingRepository on real Postgres")
class BookingRepositoryIT {

    // every query takes now/today as a parameter, so fixed values make the tests independent of the real date
    private static final Instant NOW = Instant.parse("2030-01-15T06:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2030, 1, 15);

    @Autowired
    private BookingRepository repo;

    private final UUID guestId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();

    // ---------- helpers ----------

    private static LocalDate jan(int day) {
        return LocalDate.of(2030, 1, day);
    }

    private static String newReference() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }

    /**
     * A valid, unsaved booking for this test's guest, host, listing and room.
     */
    private Booking newBooking(BookingStatus status, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal price = new BigDecimal("2000.00");
        return Booking.builder()
                .reference(newReference())
                .guestId(guestId).hostId(hostId).listingId(listingId).roomId(roomId)
                .checkIn(checkIn).checkOut(checkOut).nights(nights)
                .roomsBooked(1).guests(2)
                .status(status)
                .pricePerNight(price).totalAmount(price.multiply(BigDecimal.valueOf(nights)))
                .currency("INR")
                .listingTitle("Test listing").roomType("DOUBLE")
                .build();
    }

    private Booking save(Booking booking) {
        return repo.saveAndFlush(booking);
    }

    private Booking saved(BookingStatus status, LocalDate checkIn, LocalDate checkOut) {
        return save(newBooking(status, checkIn, checkOut));
    }

    private Booking savedHold(LocalDate checkIn, LocalDate checkOut, Instant holdExpiresAt) {
        Booking hold = newBooking(PENDING_PAYMENT, checkIn, checkOut);
        hold.setHoldExpiresAt(holdExpiresAt);
        return save(hold);
    }

    /**
     * Reads the row again from the database (bulk updates skip objects already in memory).
     */
    private Booking reload(Booking booking) {
        return repo.findById(booking.getId()).orElseThrow();
    }

    // ---------- availability ----------

    @Nested
    @DisplayName("findActiveOverlapping")
    class FindActiveOverlapping {

        @ParameterizedTest(name = "existing stay 20→23, new stay {0}→{1}: overlaps = {2}")
        @CsvSource({
                "18, 20, false", // new stay ends on the existing check-in day
                "18, 21, true",
                "21, 22, true",  // inside
                "19, 24, true",  // covers it
                "22, 25, true",
                "23, 25, false"  // new stay starts on the existing check-out day
        })
        void confirmedBooking_blocksOnlyNightsThatOverlap(int from, int to, boolean overlaps) {
            saved(CONFIRMED, jan(20), jan(23));

            List<Booking> found = repo.findActiveOverlapping(roomId, jan(from), jan(to), NOW);

            assertThat(found).hasSize(overlaps ? 1 : 0);
        }

        @Test
        void holdThatHasNotExpired_blocksTheRoom() {
            savedHold(jan(20), jan(22), NOW.plus(Duration.ofMinutes(10)));

            assertThat(repo.findActiveOverlapping(roomId, jan(20), jan(22), NOW)).hasSize(1);
        }

        @Test
        void expiredHold_doesNotBlockTheRoom() {
            savedHold(jan(20), jan(22), NOW.minus(Duration.ofMinutes(1)));

            assertThat(repo.findActiveOverlapping(roomId, jan(20), jan(22), NOW)).isEmpty();
        }

        @Test
        @DisplayName("a hold expiring exactly now no longer blocks (holdExpiresAt > now is strict)")
        void holdExpiringExactlyNow_doesNotBlockTheRoom() {
            savedHold(jan(20), jan(22), NOW);

            assertThat(repo.findActiveOverlapping(roomId, jan(20), jan(22), NOW)).isEmpty();
        }

        @ParameterizedTest(name = "a {0} booking does not block the room")
        @EnumSource(value = BookingStatus.class, names = {"CANCELLED", "EXPIRED", "COMPLETED"})
        void finishedBookings_doNotBlockTheRoom(BookingStatus status) {
            saved(status, jan(20), jan(22));

            assertThat(repo.findActiveOverlapping(roomId, jan(20), jan(22), NOW)).isEmpty();
        }

        @Test
        void bookingOfAnotherRoom_isIgnored() {
            Booking otherRoom = newBooking(CONFIRMED, jan(20), jan(22));
            otherRoom.setRoomId(UUID.randomUUID());
            save(otherRoom);

            assertThat(repo.findActiveOverlapping(roomId, jan(20), jan(22), NOW)).isEmpty();
        }
    }

    @Nested
    @DisplayName("other availability queries")
    class OtherAvailability {

        @Test
        @DisplayName("findActiveOverlappingForListing: every room of the listing, never another listing")
        void findActiveOverlappingForListing_coversAllRoomsOfTheListing() {
            Booking otherRoom = newBooking(CONFIRMED, jan(20), jan(22));
            otherRoom.setRoomId(UUID.randomUUID());
            otherRoom = save(otherRoom);
            Booking otherListing = newBooking(CONFIRMED, jan(20), jan(22));
            otherListing.setListingId(UUID.randomUUID());
            save(otherListing);

            List<Booking> found = repo.findActiveOverlappingForListing(listingId, jan(20), jan(22), NOW);

            assertThat(found).extracting(Booking::getId).containsExactly(otherRoom.getId());
        }

        @Test
        @DisplayName("findActiveFromDate: a stay ending today is no longer in the room's future")
        void findActiveFromDate_skipsStaysThatEndToday() {
            saved(CONFIRMED, jan(13), TODAY);
            Booking future = saved(CONFIRMED, jan(14), jan(16));

            List<Booking> found = repo.findActiveFromDate(roomId, TODAY, NOW);

            assertThat(found).extracting(Booking::getId).containsExactly(future.getId());
        }
    }

    // ---------- counts ----------

    @Nested
    @DisplayName("count queries")
    class Counts {

        @Test
        void countActiveForRoom_countsStaysEndingTodayOrLater() {
            saved(CONFIRMED, jan(13), TODAY);                               // ends today: counted (>=)
            saved(CONFIRMED, jan(20), jan(22));                             // future: counted
            saved(CONFIRMED, jan(10), jan(14));                             // ended yesterday
            savedHold(jan(20), jan(22), NOW.minus(Duration.ofMinutes(1)));  // expired hold

            assertThat(repo.countActiveForRoom(roomId, TODAY, NOW)).isEqualTo(2);
        }

        @Test
        void countActiveForGuest_countsOnlyThatGuest() {
            saved(CONFIRMED, jan(20), jan(22));
            Booking otherGuest = newBooking(CONFIRMED, jan(20), jan(22));
            otherGuest.setGuestId(UUID.randomUUID());
            save(otherGuest);

            assertThat(repo.countActiveForGuest(guestId, TODAY, NOW)).isEqualTo(1);
        }

        @Test
        void countActiveForHost_countsOnlyThatHost() {
            saved(CONFIRMED, jan(20), jan(22));
            Booking otherHost = newBooking(CONFIRMED, jan(20), jan(22));
            otherHost.setHostId(UUID.randomUUID());
            save(otherHost);

            assertThat(repo.countActiveForHost(hostId, TODAY, NOW)).isEqualTo(1);
        }

        @Test
        void countActiveForListing_countsEveryRoomAndLiveHolds() {
            saved(CONFIRMED, jan(20), jan(22));
            Booking otherRoom = newBooking(CONFIRMED, jan(20), jan(22));
            otherRoom.setRoomId(UUID.randomUUID());
            save(otherRoom);
            savedHold(jan(24), jan(25), NOW.plus(Duration.ofMinutes(10)));

            assertThat(repo.countActiveForListing(listingId, TODAY, NOW)).isEqualTo(3);
        }

        @Test
        void countPendingHolds_countsOnlyLiveHoldsOfTheGuest() {
            savedHold(jan(20), jan(21), NOW.plus(Duration.ofMinutes(5)));
            savedHold(jan(22), jan(23), NOW.plus(Duration.ofMinutes(9)));
            savedHold(jan(24), jan(25), NOW.minus(Duration.ofMinutes(1)));  // expired
            saved(CONFIRMED, jan(26), jan(27));                             // paid, not a hold
            Booking otherGuest = newBooking(PENDING_PAYMENT, jan(20), jan(21));
            otherGuest.setGuestId(UUID.randomUUID());
            otherGuest.setHoldExpiresAt(NOW.plus(Duration.ofMinutes(5)));
            save(otherGuest);

            assertThat(repo.countPendingHolds(guestId, NOW)).isEqualTo(2);
        }
    }

    // ---------- reference ----------

    @Nested
    @DisplayName("booking reference")
    class Reference {

        @Test
        void existsByReference_isTrueOnlyForAStoredReference() {
            Booking booking = saved(CONFIRMED, jan(20), jan(22));

            assertThat(repo.existsByReference(booking.getReference())).isTrue();
            assertThat(repo.existsByReference("TNOTSTORED")).isFalse();
        }

        @Test
        void duplicateReference_isRejectedByTheDatabase() {
            Booking first = saved(CONFIRMED, jan(20), jan(22));
            Booking copy = newBooking(CONFIRMED, jan(24), jan(26));
            copy.setReference(first.getReference());

            assertThatThrownBy(() -> repo.saveAndFlush(copy))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // ---------- bulk updates (scheduled jobs) ----------

    @Nested
    @DisplayName("expireHolds")
    class ExpireHolds {

        @Test
        void expiresOnlyHoldsWhoseTimeHasPassed() {
            Booking expired = savedHold(jan(20), jan(22), NOW.minus(Duration.ofMinutes(1)));
            Booking live = savedHold(jan(24), jan(26), NOW.plus(Duration.ofMinutes(5)));
            Booking paid = saved(CONFIRMED, jan(27), jan(28));

            int changed = repo.expireHolds(NOW);

            assertThat(changed).isEqualTo(1);
            Booking reloaded = reload(expired);
            assertThat(reloaded.getStatus()).isEqualTo(EXPIRED);
            assertThat(reloaded.getVersion()).isEqualTo(1L); // bumped: a stale copy now fails with an optimistic-lock error
            assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
            assertThat(reload(live).getStatus()).isEqualTo(PENDING_PAYMENT);
            assertThat(reload(paid).getStatus()).isEqualTo(CONFIRMED);
        }

        @Test
        @DisplayName("a hold expiring exactly now is left alone (holdExpiresAt < now is strict); the next run expires it")
        void holdExpiringExactlyNow_isNotExpiredYet() {
            Booking hold = savedHold(jan(20), jan(22), NOW);

            assertThat(repo.expireHolds(NOW)).isZero();
            assertThat(reload(hold).getStatus()).isEqualTo(PENDING_PAYMENT);
        }
    }

    @Test
    @DisplayName("completeFinished: only confirmed stays that ended before today become COMPLETED")
    void completeFinished_completesOnlyConfirmedStaysThatEndedBeforeToday() {
        Booking ended = saved(CONFIRMED, jan(12), jan(14));       // left yesterday
        Booking leavingToday = saved(CONFIRMED, jan(13), TODAY);  // checks out today
        Booking cancelled = saved(CANCELLED, jan(12), jan(14));

        int changed = repo.completeFinished(TODAY, NOW);

        assertThat(changed).isEqualTo(1);
        Booking completed = reload(ended);
        assertThat(completed.getStatus()).isEqualTo(COMPLETED);
        assertThat(completed.getVersion()).isEqualTo(1L);
        assertThat(reload(leavingToday).getStatus()).isEqualTo(CONFIRMED);
        assertThat(reload(cancelled).getStatus()).isEqualTo(CANCELLED);
    }

    // ---------- calendar, locks, reports ----------

    @Test
    @DisplayName("findForCalendar: confirmed, completed and live holds inside [from, to), sorted by check-in")
    void findForCalendar_returnsWhatOccupiesTheRange_sortedByCheckIn() {
        Booking completed = saved(COMPLETED, jan(18), jan(21));                          // started before, overlaps
        Booking hold = savedHold(jan(22), jan(23), NOW.plus(Duration.ofMinutes(5)));
        Booking confirmed = saved(CONFIRMED, jan(25), jan(27));
        saved(CANCELLED, jan(23), jan(24));
        savedHold(jan(26), jan(28), NOW.minus(Duration.ofMinutes(1)));                   // expired hold
        saved(CONFIRMED, LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 3));           // starts on the end date
        Booking otherListing = newBooking(CONFIRMED, jan(24), jan(25));
        otherListing.setListingId(UUID.randomUUID());
        save(otherListing);

        List<Booking> found = repo.findForCalendar(listingId, jan(20), LocalDate.of(2030, 2, 1), NOW);

        assertThat(found).extracting(Booking::getId)
                .containsExactly(completed.getId(), hold.getId(), confirmed.getId());
    }

    @Test
    void findPaymentIdById_returnsThePaymentId() {
        UUID paymentId = UUID.randomUUID();
        Booking booking = newBooking(CONFIRMED, jan(20), jan(22));
        booking.setPaymentId(paymentId);
        booking = save(booking);

        assertThat(repo.findPaymentIdById(booking.getId())).isEqualTo(paymentId);
    }

    @Test
    @DisplayName("findByIdForUpdate: the SELECT ... FOR UPDATE runs on Postgres and returns the row")
    void findByIdForUpdate_returnsTheRow() {
        Booking booking = saved(CONFIRMED, jan(20), jan(22));

        assertThat(repo.findByIdForUpdate(booking.getId()))
                .map(Booking::getReference)
                .contains(booking.getReference());
    }

    @Test
    @DisplayName("countByStatusBetween: groups only bookings created inside the range (backdate moves createdAt)")
    void countByStatusBetween_groupsBookingsCreatedInTheRange() {
        Booking old = saved(CONFIRMED, jan(20), jan(22));
        saved(CANCELLED, jan(23), jan(24)); // created now: outside the range
        repo.backdate(old.getId(), Instant.parse("2020-01-10T10:00:00Z"));

        List<Object[]> rows = repo.countByStatusBetween(
                Instant.parse("2020-01-01T00:00:00Z"), Instant.parse("2020-02-01T00:00:00Z"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly(CONFIRMED, 1L);
    }
}