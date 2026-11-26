package blinkstay.flow;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.earnings.enums.EarningsStatus;
import blinkstay.payment.entities.Payment;
import blinkstay.payment.enums.PaymentStatus;
import blinkstay.payment.enums.ProviderEventType;
import blinkstay.payment.gateway.ProviderEvent;
import blinkstay.payment.repository.PaymentEventRepository;
import blinkstay.payment.service.PaymentEventProcessor;
import blinkstay.payment.serviceImpl.RefundProcessor;
import blinkstay.room.entities.ListingRoom;
import blinkstay.support.Concurrently;
import blinkstay.support.Concurrently.Outcome;
import blinkstay.support.FlowTestSupport;
import blinkstay.support.IntegrationTest;
import blinkstay.support.TestDates;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@DisplayName("Concurrency on real Postgres: last room, duplicate payment events, double cancel, refund race")
class ConcurrencyIT extends FlowTestSupport {

    @Autowired
    private PaymentEventProcessor paymentEvents;

    @Autowired
    private RefundProcessor refundProcessor;

    @Autowired
    private PaymentEventRepository eventRepo;

    private User host;

    @BeforeEach
    void setUp() {
        host = newUser(UserRole.HOTEL_MANAGER);
    }

    // ---------- helpers ----------

    private List<User> newGuests(int count) {
        return IntStream.range(0, count).mapToObj(i -> newUser(UserRole.USER)).toList();
    }

    private static long countStatus(List<Outcome<Integer>> outcomes, int httpStatus) {
        return outcomes.stream().filter(o -> Integer.valueOf(httpStatus).equals(o.value())).count();
    }

    private long eventsStoredFor(String orderId) {
        return eventRepo.findAll().stream().filter(e -> e.getOrderId().equals(orderId)).count();
    }

    private long earningsRowsFor(UUID bookingId) {
        return earningsRepo.findAll().stream().filter(e -> e.getBookingId().equals(bookingId)).count();
    }

    private Payment payment(UUID paymentId) {
        return paymentRepo.findById(paymentId).orElseThrow();
    }

    // ---------- last room ----------

    @Nested
    @DisplayName("booking the same room at the same moment")
    class LastRoom {

        @RepeatedTest(value = 3, name = "{displayName} (run {currentRepetition} of {totalRepetitions})")
        @DisplayName("five guests try to book the last room at once: exactly one gets it")
        void fiveGuestsAtOnce_exactlyOneGetsTheLastRoom() throws Exception {
            ListingRoom room = newPublishedListingWithRoom(host, 1);
            List<User> guests = newGuests(5);

            List<Outcome<Integer>> outcomes = Concurrently.<Integer>runTogether(5, i -> () ->
                    createBooking(guests.get(i), room, inDays(30), inDays(32)).andReturn().getResponse().getStatus());

            assertThat(outcomes).noneMatch(Outcome::failed);
            assertThat(countStatus(outcomes, 201)).isEqualTo(1);
            assertThat(countStatus(outcomes, 409)).isEqualTo(4);
            assertThat(bookingRepo.countActiveForRoom(room.getId(), TestDates.today(), Instant.now())).isEqualTo(1);
        }

        @Test
        @DisplayName("four guests, two rooms left: exactly two get one")
        void fourGuestsTwoRooms_exactlyTwoSucceed() throws Exception {
            ListingRoom room = newPublishedListingWithRoom(host, 2);
            List<User> guests = newGuests(4);

            List<Outcome<Integer>> outcomes = Concurrently.<Integer>runTogether(4, i -> () ->
                    createBooking(guests.get(i), room, inDays(30), inDays(32)).andReturn().getResponse().getStatus());

            assertThat(outcomes).noneMatch(Outcome::failed);
            assertThat(countStatus(outcomes, 201)).isEqualTo(2);
            assertThat(countStatus(outcomes, 409)).isEqualTo(2);
            assertThat(bookingRepo.countActiveForRoom(room.getId(), TestDates.today(), Instant.now())).isEqualTo(2);
        }
    }

    // ---------- duplicate payment events ----------

    @Nested
    @DisplayName("the same payment reported twice at the same moment")
    class DuplicatePaymentEvents {

        private User guest;
        private UUID bookingId;
        private String orderId;
        private UUID paymentId;

        @BeforeEach
        void bookAndStartPayment() throws Exception {
            guest = newUser(UserRole.USER);
            ListingRoom room = newPublishedListingWithRoom(host, 1);
            bookingId = bookingIdOf(createBooking(guest, room, inDays(30), inDays(32)).andExpect(status().isCreated()));
            ResultActions session = startPayment(guest, bookingId).andExpect(status().isOk());
            orderId = read(session, "$.data.orderId");
            paymentId = UUID.fromString(read(session, "$.data.paymentId"));
        }

        private ProviderEvent succeeded(String eventId) {
            Payment p = payment(paymentId);
            return new ProviderEvent(eventId, ProviderEventType.PAYMENT_SUCCEEDED, orderId, "mock_pay_" + paymentId,
                    p.getAmount(), p.getCurrency(), null);
        }

        @Test
        @DisplayName("same event id twice (provider retry): confirmed once, event stored once, one earnings row")
        void sameEventTwiceAtOnce_isProcessedOnce() throws Exception {
            ProviderEvent event = succeeded("evt_" + UUID.randomUUID());

            List<Outcome<Void>> outcomes = Concurrently.<Void>runTogether(2, i -> () -> {
                paymentEvents.process("MOCK", event);
                return null;
            });

            assertThat(outcomes).noneMatch(Outcome::failed);
            assertThat(payment(paymentId).getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(booking(bookingId).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(eventsStoredFor(orderId)).isEqualTo(1);
            assertThat(earningsRowsFor(bookingId)).isEqualTo(1);
        }

        @Test
        @DisplayName("browser verify and webhook together (two event ids): both remembered, confirmed once")
        void verifyAndWebhookAtOnce_confirmOnce() throws Exception {
            List<ProviderEvent> events = List.of(succeeded("verify_" + UUID.randomUUID()),
                    succeeded("payment.captured_" + UUID.randomUUID()));

            List<Outcome<Void>> outcomes = Concurrently.<Void>runTogether(2, i -> () -> {
                paymentEvents.process("MOCK", events.get(i));
                return null;
            });

            assertThat(outcomes).noneMatch(Outcome::failed);
            assertThat(payment(paymentId).getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(booking(bookingId).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(eventsStoredFor(orderId)).isEqualTo(2);
            assertThat(earningsRowsFor(bookingId)).isEqualTo(1);
        }
    }

    // ---------- refunds ----------

    @Nested
    @DisplayName("refunds requested or sent at the same moment")
    class Refunds {

        private User guest;
        private UUID bookingId;
        private UUID paymentId;

        @BeforeEach
        void paidBooking() throws Exception {
            guest = newUser(UserRole.USER);
            ListingRoom room = newPublishedListingWithRoom(host, 1);
            bookingId = confirmedBooking(guest, room, inDays(30), inDays(32));
            paymentId = paymentRepo
                    .findFirstByBookingIdAndStatusOrderByCreatedAtDesc(bookingId, PaymentStatus.SUCCEEDED)
                    .orElseThrow().getId();
        }

        @Test
        @DisplayName("double-click on Cancel: the refund is requested and sent once")
        void doubleClickCancel_refundsOnce() throws Exception {
            List<Outcome<Integer>> outcomes = Concurrently.<Integer>runTogether(2, i -> () ->
                    cancel(guest, bookingId).andReturn().getResponse().getStatus());

            assertThat(outcomes).noneMatch(Outcome::failed);
            assertThat(countStatus(outcomes, 200)).isGreaterThanOrEqualTo(1); // 200 or 409 for the 2nd click is a UX choice
            assertThat(outcomes).allMatch(o -> o.value() < 500);

            waitUntil("the refund is sent", () -> payment(paymentId).getStatus() == PaymentStatus.REFUNDED);
            assertThat(payment(paymentId).getRefundAttempts()).isEqualTo(1);
            assertThat(payment(paymentId).getRefundedAmount()).isEqualByComparingTo("4000.00");
            assertThat(booking(bookingId).getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(earningsFor(bookingId).getStatus()).isEqualTo(EarningsStatus.REVERSED);
        }

        @Test
        @DisplayName("the async listener and three timer runs try the same refund: it is sent once")
        void listenerAndTimersRace_sendTheRefundOnce() throws Exception {
            cancel(guest, bookingId).andExpect(status().isOk()); // starts the @Async listener's attempt

            List<Outcome<Void>> outcomes = Concurrently.<Void>runTogether(3, i -> () -> {
                refundProcessor.attempt(paymentId); // what RefundTimers would call
                return null;
            });

            assertThat(outcomes).noneMatch(Outcome::failed);
            waitUntil("the refund is sent", () -> payment(paymentId).getStatus() == PaymentStatus.REFUNDED);
            assertThat(payment(paymentId).getRefundAttempts()).isEqualTo(1);
        }
    }
}