package com.blinkstay.booking.serviceImpl;

import com.blinkstay.booking.config.BookingProperties;
import com.blinkstay.booking.dto.CancellationPreviewDto;
import com.blinkstay.booking.entities.Booking;
import com.blinkstay.booking.enums.BookingStatus;
import com.blinkstay.booking.mapper.BookingMapper;
import com.blinkstay.booking.repository.BookingRepository;
import com.blinkstay.booking.service.BookingRules;
import com.blinkstay.booking.service.CancellationPolicy;
import com.blinkstay.common.payment.BookingCancelledEvent;
import com.blinkstay.common.payment.PaymentRefunds;
import com.blinkstay.support.TestDates;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static com.blinkstay.support.builders.BookingBuilder.aBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Cancellation rules (Q52, Q53): who may cancel what, how much is refunded, and the lock order
 * (payment row first, then booking row).
 * <p>
 * "Sociable" unit test: the real BookingRules and CancellationPolicy are used (pure rules, already
 * tested), while the repository, the payment module, the mapper and the event publisher are mocks.
 */
@ExtendWith(MockitoExtension.class)
class BookingCancellationServiceImplTest {

    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private BookingMapper mapper;
    @Mock
    private PaymentRefunds payments;
    @Mock
    private ApplicationEventPublisher publisher;

    private BookingCancellationServiceImpl service;
    private LocalDate today;

    private final UUID guestId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        BookingProperties props = new BookingProperties();
        BookingRules rules = new BookingRules(props);
        service = new BookingCancellationServiceImpl(bookingRepo, rules, new CancellationPolicy(props, rules),
                mapper, payments, publisher);
        today = TestDates.today();
    }

    // ---------------- guest ----------------

    @Nested
    @DisplayName("guest cancels")
    class Guest {

        @Test
        void unpaidHold_isCancelledForFree_andPaymentModuleIsNotTouched() {
            Booking b = aBooking().withId().guest(guestId).pendingPayment(Duration.ofMinutes(10)).build();
            arrange(b, null);
            stubSave();

            service.cancelByGuest(guestId, b.getId(), null);

            assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(b.getRefundAmount()).isNull(); // null = nothing was paid
            assertThat(b.getCancelledBy()).isEqualTo(guestId);
            assertThat(b.getCancelledAt()).isNotNull();
            verifyNoInteractions(payments);

            BookingCancelledEvent event = publishedEvent();
            assertThat(event.wasPaid()).isFalse();
            assertThat(event.refundAmount()).isNull();
            assertThat(event.cancelledBy()).isEqualTo("GUEST");
            assertThat(event.reason()).isEqualTo("Cancelled by the guest");
        }

        @Test
        void paidBooking_longBeforeDeadline_isRefundedInFull() {
            Booking b = confirmedForGuest(today.plusDays(10)); // total 4000.00
            arrange(b, paymentId);
            when(payments.refundInFull(paymentId, "Booking " + b.getReference() + " was cancelled"))
                    .thenReturn(new BigDecimal("4000.00"));
            stubSave();

            service.cancelByGuest(guestId, b.getId(), "Change of plans");

            assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(b.getRefundAmount()).isEqualByComparingTo("4000.00");
            assertThat(b.getCancellationReason()).isEqualTo("Change of plans");
            BookingCancelledEvent event = publishedEvent();
            assertThat(event.wasPaid()).isTrue();
            assertThat(event.refundAmount()).isEqualByComparingTo("4000.00");
        }

        @Test
        void locksPaymentRowBeforeBookingRow() {
            // the fixed lock order that prevents deadlocks with the payment processor (Q76)
            Booking b = confirmedForGuest(today.plusDays(10));
            arrange(b, paymentId);
            when(payments.refundInFull(any(), anyString())).thenReturn(new BigDecimal("4000.00"));
            stubSave();

            service.cancelByGuest(guestId, b.getId(), null);

            InOrder order = inOrder(payments, bookingRepo);
            order.verify(payments).lockPayment(paymentId);
            order.verify(bookingRepo).findByIdForUpdate(b.getId());
        }

        @Test
        void paidBooking_afterFreeCancellationEnded_isCancelledWithoutRefund() {
            // check-in tomorrow: the 48 h window closed yesterday
            Booking b = confirmedForGuest(today.plusDays(1));
            arrange(b, paymentId);
            stubSave();

            service.cancelByGuest(guestId, b.getId(), null);

            assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(b.getRefundAmount()).isEqualByComparingTo("0");
            verify(payments, never()).refundInFull(any(), any());
            BookingCancelledEvent event = publishedEvent();
            assertThat(event.wasPaid()).isTrue(); // the host keeps the money (EarningsLedger)
            assertThat(event.refundAmount()).isEqualByComparingTo("0");
        }

        @Test
        void onCheckInDay_isRejected_andNothingChanges() {
            Booking b = confirmedForGuest(today);
            arrange(b, paymentId);

            assertError(() -> service.cancelByGuest(guestId, b.getId(), null),
                    HttpStatus.CONFLICT, "The stay has already started. Please contact the host");

            assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(bookingRepo, never()).save(any());
            verifyNoInteractions(publisher);
        }

        @Test
        void someoneElsesBooking_isNotFound() {
            Booking b = aBooking().withId().guest(UUID.randomUUID()).build();
            arrange(b, null);

            assertError(() -> service.cancelByGuest(guestId, b.getId(), null),
                    HttpStatus.NOT_FOUND, "Booking not found");
        }

        @Test
        void confirmedBookingWithoutPayment_isAnInternalError() {
            // data that should never exist: paid status but no payment row to refund
            Booking b = aBooking().withId().guest(guestId).confirmed().checkIn(today.plusDays(10)).build();
            arrange(b, null);

            assertThatThrownBy(() -> service.cancelByGuest(guestId, b.getId(), null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("has no payment");
        }
    }

    // ---------------- host and admin ----------------

    @Nested
    @DisplayName("host and admin cancel")
    class HostAndAdmin {

        @ParameterizedTest(name = "reason [{0}] -> 400")
        @ValueSource(strings = {"", "   "})
        void hostWithoutReason_isBadRequest_beforeAnythingIsRead(String reason) {
            assertError(() -> service.cancelByHost(hostId, UUID.randomUUID(), reason),
                    HttpStatus.BAD_REQUEST, "A reason is required");
            verifyNoInteractions(bookingRepo, payments, publisher);
        }

        @Test
        void hostWithNullReason_isBadRequest() {
            assertError(() -> service.cancelByHost(hostId, UUID.randomUUID(), null),
                    HttpStatus.BAD_REQUEST, "A reason is required");
        }

        @Test
        void hostCannotCancelAnUnpaidHold() {
            Booking b = aBooking().withId().host(hostId).pendingPayment(Duration.ofMinutes(10)).build();
            arrange(b, null);

            assertError(() -> service.cancelByHost(hostId, b.getId(), "Overbooked"),
                    HttpStatus.CONFLICT, "Only paid bookings can be cancelled by a host");
        }

        @Test
        void hostCancelsPaidBooking_alwaysFullRefund_evenAfterGuestDeadline() {
            Booking b = aBooking().withId().host(hostId).confirmed().payment(paymentId)
                    .checkIn(today.plusDays(1)).build(); // guest would get nothing now
            arrange(b, paymentId);
            when(payments.refundInFull(any(), anyString())).thenReturn(new BigDecimal("4000.00"));
            stubSave();

            service.cancelByHost(hostId, b.getId(), "  Property flooded  ");

            assertThat(b.getRefundAmount()).isEqualByComparingTo("4000.00");
            assertThat(b.getCancellationReason()).isEqualTo("Property flooded"); // trimmed
            assertThat(b.getCancelledBy()).isEqualTo(hostId);
            assertThat(publishedEvent().cancelledBy()).isEqualTo("HOST");
        }

        @Test
        void hostCanCancelDuringTheStay_withFullRefund() {
            // documents current behaviour: check-in yesterday, check-out tomorrow
            Booking b = aBooking().withId().host(hostId).confirmed().payment(paymentId)
                    .checkIn(today.minusDays(1)).nights(2).build();
            arrange(b, paymentId);
            when(payments.refundInFull(any(), anyString())).thenReturn(new BigDecimal("4000.00"));
            stubSave();

            service.cancelByHost(hostId, b.getId(), "Emergency repair");

            assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(b.getRefundAmount()).isEqualByComparingTo("4000.00");
        }

        @Test
        void hostCannotCancelAStayThatHasEnded() {
            // checked out yesterday, not yet marked COMPLETED by the timer
            Booking b = aBooking().withId().host(hostId).confirmed().payment(paymentId)
                    .checkIn(today.minusDays(3)).nights(2).build();
            arrange(b, paymentId);

            assertError(() -> service.cancelByHost(hostId, b.getId(), "Late"),
                    HttpStatus.CONFLICT, "This stay has already ended");
        }

        @Test
        void hostCannotCancelAnotherHostsBooking() {
            Booking b = aBooking().withId().host(UUID.randomUUID()).confirmed().payment(paymentId).build();
            arrange(b, paymentId);

            assertError(() -> service.cancelByHost(hostId, b.getId(), "Reason"),
                    HttpStatus.NOT_FOUND, "Booking not found");
        }

        @Test
        void adminCanCancelAnyBooking_withFullRefund() {
            Booking b = aBooking().withId().confirmed().payment(paymentId).build(); // random guest and host
            arrange(b, paymentId);
            when(payments.refundInFull(any(), anyString())).thenReturn(new BigDecimal("4000.00"));
            stubSave();

            service.cancelByAdmin(adminId, b.getId(), "Fraud check");

            assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(b.getCancelledBy()).isEqualTo(adminId);
            assertThat(publishedEvent().cancelledBy()).isEqualTo("ADMIN");
        }
    }

    // ---------------- states and races ----------------

    @Nested
    @DisplayName("states and races")
    class StatesAndRaces {

        @ParameterizedTest(name = "{0} -> 409")
        @EnumSource(value = BookingStatus.class, names = {"CANCELLED", "EXPIRED", "COMPLETED"})
        void finishedBookings_cannotBeCancelledAgain(BookingStatus status) {
            Booking b = aBooking().withId().guest(guestId).status(status).build();
            arrange(b, null);

            assertError(() -> service.cancelByGuest(guestId, b.getId(), null), HttpStatus.CONFLICT,
                    "This booking cannot be cancelled: it is " + status.name().toLowerCase().replace('_', ' '));
        }

        @Test
        void paymentConfirmedBetweenTheTwoReads_isConflict_tryAgain() {
            // first read: no payment yet; locked read: a payment was attached meanwhile (Q53)
            Booking b = confirmedForGuest(today.plusDays(10));
            arrange(b, null);

            assertError(() -> service.cancelByGuest(guestId, b.getId(), null),
                    HttpStatus.CONFLICT, "The booking just changed. Please try again.");
            verify(bookingRepo, never()).save(any());
        }
    }

    // ---------------- preview ----------------

    @Nested
    @DisplayName("preview (what would happen if the guest cancelled now)")
    class Preview {

        @Test
        void paidBookingEarly_showsFullRefundAndDeadline() {
            Booking b = confirmedForGuest(today.plusDays(10));
            when(bookingRepo.findById(b.getId())).thenReturn(Optional.of(b));

            CancellationPreviewDto p = service.preview(guestId, b.getId());

            assertThat(p.isCancellable()).isTrue();
            assertThat(p.getRefundAmount()).isEqualByComparingTo("4000.00");
            assertThat(p.getFreeCancellationUntil()).isNotNull();
            assertThat(p.getMessage()).contains("full refund of INR 4000.00");
        }

        @Test
        void unpaidHold_costsNothing_andHasNoDeadline() {
            Booking b = aBooking().withId().guest(guestId).pendingPayment(Duration.ofMinutes(10)).build();
            when(bookingRepo.findById(b.getId())).thenReturn(Optional.of(b));

            CancellationPreviewDto p = service.preview(guestId, b.getId());

            assertThat(p.isCancellable()).isTrue();
            assertThat(p.getRefundAmount()).isEqualByComparingTo("0");
            assertThat(p.getFreeCancellationUntil()).isNull();
            assertThat(p.getMessage()).contains("not paid yet");
        }

        @Test
        void paidBookingAfterDeadline_saysNoRefund() {
            Booking b = confirmedForGuest(today.plusDays(1));
            when(bookingRepo.findById(b.getId())).thenReturn(Optional.of(b));

            CancellationPreviewDto p = service.preview(guestId, b.getId());

            assertThat(p.isCancellable()).isTrue();
            assertThat(p.getRefundAmount()).isEqualByComparingTo("0");
            assertThat(p.getMessage()).contains("no refund");
        }

        @Test
        void checkInDay_isNotCancellable() {
            Booking b = confirmedForGuest(today);
            when(bookingRepo.findById(b.getId())).thenReturn(Optional.of(b));

            CancellationPreviewDto p = service.preview(guestId, b.getId());

            assertThat(p.isCancellable()).isFalse();
            assertThat(p.getMessage()).contains("already started");
        }

        @Test
        void someoneElsesBooking_isNotFound() {
            Booking b = aBooking().withId().guest(UUID.randomUUID()).build();
            when(bookingRepo.findById(b.getId())).thenReturn(Optional.of(b));

            assertError(() -> service.preview(guestId, b.getId()), HttpStatus.NOT_FOUND, "Booking not found");
        }
    }

    // ---------------- helpers ----------------

    private Booking confirmedForGuest(LocalDate checkIn) {
        return aBooking().withId().guest(guestId).confirmed().payment(paymentId).checkIn(checkIn).build();
    }

    /**
     * The two reads cancel() does: the plain payment-id read, then the locked booking read.
     */
    private void arrange(Booking b, UUID paymentIdOnFirstRead) {
        when(bookingRepo.findPaymentIdById(b.getId())).thenReturn(paymentIdOnFirstRead);
        when(bookingRepo.findByIdForUpdate(b.getId())).thenReturn(Optional.of(b));
    }

    private void stubSave() {
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private BookingCancelledEvent publishedEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(BookingCancelledEvent.class);
        return (BookingCancelledEvent) captor.getValue();
    }

    private static void assertError(ThrowingCallable call, HttpStatus status, String reason) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, e -> {
            assertThat(e.getStatusCode()).isEqualTo(status);
            assertThat(e.getReason()).isEqualTo(reason);
        });
    }
}