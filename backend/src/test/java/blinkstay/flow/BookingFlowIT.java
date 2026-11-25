package blinkstay.flow;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.service.BookingService;
import blinkstay.earnings.entities.EarningsEntry;
import blinkstay.earnings.enums.EarningsStatus;
import blinkstay.payment.entities.Payment;
import blinkstay.payment.enums.PaymentStatus;
import blinkstay.room.entities.ListingRoom;
import blinkstay.support.FlowTestSupport;
import blinkstay.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@DisplayName("Booking flow end to end: book → pay → confirm → cancel → refund")
class BookingFlowIT extends FlowTestSupport {

    @Autowired
    private BookingService bookingService;

    private User host;
    private User guest;
    private ListingRoom room; // 1 room of this type, 2000.00 a night

    @BeforeEach
    void setUp() {
        host = newUser(UserRole.HOTEL_MANAGER);
        guest = newUser(UserRole.USER);
        room = newPublishedListingWithRoom(host, 1);
    }

    @Nested
    @DisplayName("paying")
    class Paying {

        @Test
        void bookPayConfirm_confirmsTheBooking_andRecordsTheHostsEarnings() throws Exception {
            UUID bookingId = bookingIdOf(createBooking(guest, room, inDays(20), inDays(22))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT")));
            assertThat(booking(bookingId).getTotalAmount()).isEqualByComparingTo("4000.00"); // 2 nights x 2000

            ResultActions session = startPayment(guest, bookingId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.mock").value(true));
            String orderId = read(session, "$.data.orderId");
            UUID paymentId = UUID.fromString(read(session, "$.data.paymentId"));

            completeMock(guest, orderId, "SUCCESS")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.paymentStatus").value("SUCCEEDED"));

            getBooking(guest, bookingId).andExpect(jsonPath("$.data.status").value("CONFIRMED"));

            Payment payment = paymentRepo.findById(paymentId).orElseThrow();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(payment.getAmount()).isEqualByComparingTo("4000.00");

            // the earnings listener runs after commit on the same thread, so the row is already there
            EarningsEntry earnings = earningsFor(bookingId);
            assertThat(earnings.getStatus()).isEqualTo(EarningsStatus.PENDING);
            assertThat(earnings.getHostId()).isEqualTo(host.getId());
            assertThat(earnings.getGross()).isEqualByComparingTo("4000.00");
            assertThat(earnings.getHostAmount()).isEqualByComparingTo("3600.00"); // 10 % commission
        }

        @Test
        void payClickedTwice_reusesTheSameOrder() throws Exception {
            UUID bookingId = bookingIdOf(createBooking(guest, room, inDays(20), inDays(22)));

            String first = read(startPayment(guest, bookingId).andExpect(status().isOk()), "$.data.orderId");
            String second = read(startPayment(guest, bookingId).andExpect(status().isOk()), "$.data.orderId");

            assertThat(second).isEqualTo(first);
        }

        @Test
        void failedPayment_keepsTheHold_andANewPaymentCanConfirmIt() throws Exception {
            UUID bookingId = bookingIdOf(createBooking(guest, room, inDays(20), inDays(22)));
            String firstOrder = read(startPayment(guest, bookingId), "$.data.orderId");

            completeMock(guest, firstOrder, "FAILURE")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.paymentStatus").value("FAILED"));
            getBooking(guest, bookingId)
                    .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                    .andExpect(jsonPath("$.data.payable").value(true));

            String secondOrder = read(startPayment(guest, bookingId), "$.data.orderId");
            assertThat(secondOrder).isNotEqualTo(firstOrder);
            completeMock(guest, secondOrder, "SUCCESS")
                    .andExpect(jsonPath("$.data.paymentStatus").value("SUCCEEDED"));

            getBooking(guest, bookingId).andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        }
    }

    @Nested
    @DisplayName("cancelling and refunds")
    class CancellingAndRefunds {

        @Test
        void guestCancelsWellBeforeCheckIn_refundsInFull_reversesEarnings_andFreesTheRoom() throws Exception {
            UUID bookingId = confirmedBooking(guest, room, inDays(20), inDays(22));
            UUID paymentId = paymentRepo
                    .findFirstByBookingIdAndStatusOrderByCreatedAtDesc(bookingId, PaymentStatus.SUCCEEDED)
                    .orElseThrow().getId();

            cancel(guest, bookingId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
            assertThat(booking(bookingId).getRefundAmount()).isEqualByComparingTo("4000.00");

            // the refund is sent on another thread after the cancel commits (@Async listener)
            waitUntil("the refund is sent",
                    () -> paymentRepo.findById(paymentId).orElseThrow().getStatus() == PaymentStatus.REFUNDED);
            assertThat(paymentRepo.findById(paymentId).orElseThrow().getRefundedAmount())
                    .isEqualByComparingTo("4000.00");

            assertThat(earningsFor(bookingId).getStatus()).isEqualTo(EarningsStatus.REVERSED);

            createBooking(newUser(UserRole.USER), room, inDays(20), inDays(22))
                    .andExpect(status().isCreated()); // the only room is free again
        }

        @Test
        @DisplayName("a payment arriving after the hold expired and the room was taken is refunded, never confirmed")
        void latePaymentAfterTheRoomWasTaken_isRefunded_andDoesNotConfirm() throws Exception {
            UUID lateBooking = bookingIdOf(createBooking(guest, room, inDays(20), inDays(22)));
            ResultActions session = startPayment(guest, lateBooking);
            String orderId = read(session, "$.data.orderId");
            UUID paymentId = UUID.fromString(read(session, "$.data.paymentId"));

            holdRanOut(lateBooking);
            bookingService.expireStaleHolds();
            UUID winner = confirmedBooking(newUser(UserRole.USER), room, inDays(20), inDays(22)); // takes the only room

            completeMock(guest, orderId, "SUCCESS")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.paymentStatus")
                            .value(anyOf(equalTo("REFUND_PENDING"), equalTo("REFUNDED"))));

            waitUntil("the late payment is refunded",
                    () -> paymentRepo.findById(paymentId).orElseThrow().getStatus() == PaymentStatus.REFUNDED);
            assertThat(booking(lateBooking).getStatus()).isNotEqualTo(BookingStatus.CONFIRMED);
            assertThat(booking(winner).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(earningsRepo.existsByBookingId(lateBooking)).isFalse();
        }
    }

    @Nested
    @DisplayName("holds")
    class Holds {

        @Test
        void expiredHold_cannotBePaid_andFreesTheRoom() throws Exception {
            UUID bookingId = bookingIdOf(createBooking(guest, room, inDays(20), inDays(22)));

            holdRanOut(bookingId);
            assertThat(bookingService.expireStaleHolds()).isPositive();

            assertThat(booking(bookingId).getStatus()).isEqualTo(BookingStatus.EXPIRED);
            startPayment(guest, bookingId).andExpect(status().isConflict());
            createBooking(newUser(UserRole.USER), room, inDays(20), inDays(22))
                    .andExpect(status().isCreated());
        }

        @Test
        void guestWithTwoUnpaidHolds_cannotStartAThird() throws Exception {
            ListingRoom bigRoom = newPublishedListingWithRoom(host, 5);
            createBooking(guest, bigRoom, inDays(20), inDays(21)).andExpect(status().isCreated());
            createBooking(guest, bigRoom, inDays(22), inDays(23)).andExpect(status().isCreated());

            createBooking(guest, bigRoom, inDays(24), inDays(25)).andExpect(status().isConflict());
        }

        @Test
        void lastRoomAlreadyConfirmed_isSoldOutForOverlappingDates() throws Exception {
            confirmedBooking(guest, room, inDays(20), inDays(23));

            createBooking(newUser(UserRole.USER), room, inDays(21), inDays(22))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Sold out for these dates"));
        }
    }

    @Nested
    @DisplayName("ownership")
    class Ownership {

        @Test
        void anotherGuest_cannotSeePayOrCancelMyBooking() throws Exception {
            UUID bookingId = confirmedBooking(guest, room, inDays(20), inDays(22));
            User stranger = newUser(UserRole.USER);

            getBooking(stranger, bookingId).andExpect(status().isNotFound());
            startPayment(stranger, bookingId).andExpect(status().is4xxClientError());
            cancel(stranger, bookingId).andExpect(status().is4xxClientError());

            assertThat(booking(bookingId).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        void anotherGuest_cannotCompleteMyPaymentOrder() throws Exception {
            UUID bookingId = bookingIdOf(createBooking(guest, room, inDays(20), inDays(22)));
            ResultActions session = startPayment(guest, bookingId);
            String orderId = read(session, "$.data.orderId");
            UUID paymentId = UUID.fromString(read(session, "$.data.paymentId"));

            completeMock(newUser(UserRole.USER), orderId, "SUCCESS").andExpect(status().isNotFound());

            assertThat(paymentRepo.findById(paymentId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CREATED);
            assertThat(booking(bookingId).getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        }

        @Test
        void hostCannotBookTheirOwnListing() throws Exception {
            createBooking(host, room, inDays(20), inDays(22)).andExpect(status().isBadRequest());
        }
    }
}