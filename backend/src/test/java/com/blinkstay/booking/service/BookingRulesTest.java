package com.blinkstay.booking.service;

import com.blinkstay.booking.config.BookingProperties;
import com.blinkstay.booking.entities.Booking;
import com.blinkstay.support.TestDates;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static com.blinkstay.support.builders.BookingBuilder.aBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * validateStay: request checks (Q47 step 2). peakUsage: availability per night (Q46).
 * Plain unit test, real objects. BookingRules reads the real clock, so dates are
 * relative to TestDates.today() (backlog item 11).
 */
class BookingRulesTest {

    private BookingRules rules;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        rules = new BookingRules(new BookingProperties()); // max 30 nights, 365 days ahead
        today = TestDates.today();
    }

    @Nested
    @DisplayName("validateStay")
    class ValidateStay {

        @Test
        void validStay_returnsNumberOfNights() {
            int nights = rules.validateStay(today.plusDays(10), today.plusDays(12), 1, 2);

            assertThat(nights).isEqualTo(2);
        }

        @Test
        void checkInToday_isAllowed() {
            int nights = rules.validateStay(today, today.plusDays(1), 1, 1);

            assertThat(nights).isEqualTo(1);
        }

        @Test
        void missingDates_isBadRequest() {
            assertBadRequest(() -> rules.validateStay(null, today.plusDays(2), 1, 1),
                    "Check-in and check-out dates are required");
        }

        @ParameterizedTest(name = "rooms = {0}")
        @NullSource
        @ValueSource(ints = {0, -1, 11})
        void roomsOutside1To10_isBadRequest(Integer rooms) {
            assertBadRequest(() -> rules.validateStay(today.plusDays(1), today.plusDays(2), rooms, 1),
                    "Rooms must be between 1 and 10");
        }

        @ParameterizedTest(name = "guests = {0}")
        @NullSource
        @ValueSource(ints = {0, 51})
        void guestsOutside1To50_isBadRequest(Integer guests) {
            assertBadRequest(() -> rules.validateStay(today.plusDays(1), today.plusDays(2), 1, guests),
                    "Guests must be between 1 and 50");
        }

        @Test
        void checkInYesterday_isBadRequest() {
            assertBadRequest(() -> rules.validateStay(today.minusDays(1), today.plusDays(1), 1, 1),
                    "Check-in cannot be in the past");
        }

        @Test
        void checkOutSameDayAsCheckIn_isBadRequest() {
            assertBadRequest(() -> rules.validateStay(today.plusDays(5), today.plusDays(5), 1, 1),
                    "Check-out must be after check-in");
        }

        @Test
        void checkOutBeforeCheckIn_isBadRequest() {
            assertBadRequest(() -> rules.validateStay(today.plusDays(5), today.plusDays(3), 1, 1),
                    "Check-out must be after check-in");
        }

        @Test
        void checkInExactly365DaysAhead_isAllowed() {
            int nights = rules.validateStay(today.plusDays(365), today.plusDays(366), 1, 1);

            assertThat(nights).isEqualTo(1);
        }

        @Test
        void checkIn366DaysAhead_isBadRequest() {
            assertBadRequest(() -> rules.validateStay(today.plusDays(366), today.plusDays(367), 1, 1),
                    "Bookings open up to 365 days ahead");
        }

        @Test
        void exactly30Nights_isAllowed() {
            int nights = rules.validateStay(today.plusDays(1), today.plusDays(31), 1, 1);

            assertThat(nights).isEqualTo(30);
        }

        @Test
        void thirtyOneNights_isBadRequest() {
            assertBadRequest(() -> rules.validateStay(today.plusDays(1), today.plusDays(32), 1, 1),
                    "The maximum stay is 30 nights");
        }
    }

    @Nested
    @DisplayName("peakUsage (rooms used on the busiest night)")
    class PeakUsage {

        // Example from Q46: A = 10-12 Dec with 2 rooms, B = 11-13 Dec with 1 room.
        // Rooms used per night: 10th = 2, 11th = 3, 12th = 1.
        private final LocalDate d10 = LocalDate.of(2026, 12, 10);
        private final Booking a = aBooking().stay(d10, d10.plusDays(2)).rooms(2).build();
        private final Booking b = aBooking().stay(d10.plusDays(1), d10.plusDays(3)).rooms(1).build();

        @Test
        void overlappingBookings_busiestNightCountsBoth() {
            int peak = BookingRules.peakUsage(List.of(a, b), d10, d10.plusDays(3));

            assertThat(peak).isEqualTo(3);
        }

        @Test
        void checkOutDayIsFree_arrivalOnSomeoneElsesCheckOutDayDoesNotConflict() {
            // A leaves on the 12th, so the night of the 12th only has B
            int peak = BookingRules.peakUsage(List.of(a, b), d10.plusDays(2), d10.plusDays(3));

            assertThat(peak).isEqualTo(1);
        }

        @Test
        void requestAfterAllBookings_usesNoRooms() {
            int peak = BookingRules.peakUsage(List.of(a, b), d10.plusDays(3), d10.plusDays(5));

            assertThat(peak).isZero();
        }

        @Test
        void noBookings_usesNoRooms() {
            int peak = BookingRules.peakUsage(List.of(), d10, d10.plusDays(3));

            assertThat(peak).isZero();
        }
    }

    /**
     * The call must fail with HTTP 400 and exactly this reason.
     */
    private static void assertBadRequest(ThrowingCallable call, String reason) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).isEqualTo(reason);
                });
    }
}