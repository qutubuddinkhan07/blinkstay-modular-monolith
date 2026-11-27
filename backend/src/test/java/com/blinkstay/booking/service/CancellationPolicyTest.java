package com.blinkstay.booking.service;

import com.blinkstay.booking.config.BookingProperties;
import com.blinkstay.booking.entities.Booking;
import com.blinkstay.booking.enums.BookingStatus;
import com.blinkstay.support.TestDates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static com.blinkstay.support.builders.BookingBuilder.aBooking;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rules under test (Q52 in the interview Q&A):
 * - free cancellation ends 48 hours before the START of the check-in day, hotel time zone
 * - a guest gets a full refund only for a CONFIRMED booking cancelled before that moment
 * - a guest may cancel an unpaid booking any time, a confirmed one only before the check-in day
 * <p>
 * Plain unit test: real objects, no Spring, no database, no mocks.
 */
class CancellationPolicyTest {

    private CancellationPolicy policy;

    @BeforeEach
    void setUp() {
        BookingProperties props = new BookingProperties(); // defaults: 48 h, Asia/Kolkata
        policy = new CancellationPolicy(props, new BookingRules(props));
    }

    @Nested
    @DisplayName("freeCancellationUntil")
    class FreeCancellationUntil {

        @Test
        @DisplayName("is 48 hours before midnight of the check-in day in hotel time (IST)")
        void is48HoursBeforeStartOfCheckInDayInHotelTime() {
            // Arrange: check-in 10 Dec 2026 starts at 00:00 IST = 9 Dec 18:30 UTC
            Booking booking = aBooking().checkIn(LocalDate.of(2026, 12, 10)).build();

            // Act
            Instant deadline = policy.freeCancellationUntil(booking);

            // Assert: minus 48 h = 7 Dec 18:30 UTC
            assertThat(deadline).isEqualTo(Instant.parse("2026-12-07T18:30:00Z"));
        }
    }

    @Nested
    @DisplayName("guestRefund")
    class GuestRefund {

        private final Booking confirmed = aBooking().confirmed()
                .checkIn(LocalDate.of(2026, 12, 10)).nights(2).pricePerNight("2000.00")
                .build(); // total 4000.00
        private final Instant deadline = Instant.parse("2026-12-07T18:30:00Z");

        @Test
        void cancel_confirmedOneSecondBeforeDeadline_refundsInFull() {
            BigDecimal refund = policy.guestRefund(confirmed, deadline.minusSeconds(1));

            assertThat(refund).isEqualByComparingTo("4000.00");
        }

        @Test
        void cancel_confirmedExactlyAtDeadline_refundsNothing() {
            // "before" is strict: at the deadline itself the free window is over
            BigDecimal refund = policy.guestRefund(confirmed, deadline);

            assertThat(refund).isEqualByComparingTo("0");
        }

        @Test
        void cancel_confirmedAfterDeadline_refundsNothing() {
            BigDecimal refund = policy.guestRefund(confirmed, deadline.plus(Duration.ofHours(1)));

            assertThat(refund).isEqualByComparingTo("0");
        }

        @Test
        void cancel_confirmedLongBefore_refundsTheWholeStoredTotal() {
            Booking twoRoomsThreeNights = aBooking().confirmed()
                    .checkIn(LocalDate.of(2026, 12, 10)).nights(3).rooms(2).pricePerNight("1500.00")
                    .build(); // 1500 x 3 x 2 = 9000.00

            BigDecimal refund = policy.guestRefund(twoRoomsThreeNights, Instant.parse("2026-11-01T00:00:00Z"));

            assertThat(refund).isEqualByComparingTo("9000.00");
        }

        @ParameterizedTest(name = "status {0} -> no refund")
        @EnumSource(value = BookingStatus.class, names = "CONFIRMED", mode = EnumSource.Mode.EXCLUDE)
        void cancel_notConfirmed_refundsNothingEvenBeforeDeadline(BookingStatus status) {
            // unpaid, cancelled, expired or completed bookings have no money to give back
            Booking booking = aBooking().status(status).checkIn(LocalDate.of(2026, 12, 10)).build();

            BigDecimal refund = policy.guestRefund(booking, Instant.parse("2026-11-01T00:00:00Z"));

            assertThat(refund).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("guestCanCancel")
    class GuestCanCancel {

        @Test
        void unpaidBooking_canAlwaysBeCancelled() {
            Booking booking = aBooking().pendingPayment(Duration.ofMinutes(10))
                    .checkIn(TestDates.today()).build();

            assertThat(policy.guestCanCancel(booking)).isTrue();
        }

        @Test
        void confirmedBooking_checkInTomorrow_canBeCancelled() {
            Booking booking = aBooking().confirmed().checkIn(TestDates.today().plusDays(1)).build();

            assertThat(policy.guestCanCancel(booking)).isTrue();
        }

        @Test
        void confirmedBooking_checkInToday_cannotBeCancelled() {
            Booking booking = aBooking().confirmed().checkIn(TestDates.today()).build();

            assertThat(policy.guestCanCancel(booking)).isFalse();
        }

        @ParameterizedTest(name = "status {0} -> cannot cancel")
        @EnumSource(value = BookingStatus.class, names = {"CANCELLED", "EXPIRED", "COMPLETED"})
        void finishedBookings_cannotBeCancelled(BookingStatus status) {
            Booking booking = aBooking().status(status).checkIn(TestDates.today().plusDays(30)).build();

            assertThat(policy.guestCanCancel(booking)).isFalse();
        }
    }
}