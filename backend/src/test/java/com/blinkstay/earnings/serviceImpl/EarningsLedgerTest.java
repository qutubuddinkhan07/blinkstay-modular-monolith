package com.blinkstay.earnings.serviceImpl;

import com.blinkstay.common.booking.BookingConfirmedEvent;
import com.blinkstay.common.payment.BookingCancelledEvent;
import com.blinkstay.common.time.HotelClock;
import com.blinkstay.earnings.entities.EarningsEntry;
import com.blinkstay.earnings.enums.EarningsStatus;
import com.blinkstay.earnings.repository.EarningsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

/**
 * Earnings ledger (Q80, Q69): commission = (gross - refunded) x percent / 100, rounded half up
 * to 2 decimals; host amount = base - commission; a full refund makes the row REVERSED.
 * <p>
 * Unit test with Mockito: the repository and the clock are fakes, so no database and no Spring.
 */
@ExtendWith(MockitoExtension.class)
class EarningsLedgerTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);

    @Mock
    private EarningsRepository repo;
    @Mock
    private HotelClock clock;
    @InjectMocks
    private EarningsLedger ledger;
    @Captor
    private ArgumentCaptor<EarningsEntry> saved;

    @BeforeEach
    void setUp() {
        // commissionPercent comes from @Value on a field, which Mockito cannot fill:
        // set it the way Spring would (app.earnings.commission-percent=10)
        ReflectionTestUtils.setField(ledger, "commissionPercent", new BigDecimal("10"));
    }

    @Nested
    @DisplayName("recordConfirmed")
    class RecordConfirmed {
        @Test
        void newPaidBooking_savesPendingEntryWithTenPercentCommission() {
            // Arrange
            BookingConfirmedEvent event = confirmedEvent(UUID.randomUUID(), "4000.00");
            when(repo.existsByBookingId(event.bookingId())).thenReturn(false);
            when(clock.today()).thenReturn(TODAY);

            // Act
            ledger.recordConfirmed(event);

            // Assert
            verify(repo).save(saved.capture());
            EarningsEntry entry = saved.getValue();
            assertThat(entry.getBookingId()).isEqualTo(event.bookingId());
            assertThat(entry.getGross()).isEqualByComparingTo("4000.00");
            assertThat(entry.getRefunded()).isEqualByComparingTo("0");
            assertThat(entry.getCommissionPercent()).isEqualByComparingTo("10");
            assertThat(entry.getCommission()).isEqualByComparingTo("400.00");
            assertThat(entry.getHostAmount()).isEqualByComparingTo("3600.00");
            assertThat(entry.getStatus()).isEqualTo(EarningsStatus.PENDING);
            assertThat(entry.getSoldOn()).isEqualTo(TODAY);
        }

        @ParameterizedTest(name = "gross {0} -> commission {1}, host {2}")
        @CsvSource({
                "4000.00, 400.00, 3600.00",
                "1234.55, 123.46, 1111.09", // 123.455 rounds up to 123.46
                "999.99, 100.00, 899.99", // 99.999 -> 100.00
                "0.05, 0.01, 0.04" // 0.005 -> 0.01 (half up)
        })
        void commission_isRoundedHalfUpToTwoDecimals(String gross, String commission, String host) {
            BookingConfirmedEvent event = confirmedEvent(UUID.randomUUID(), gross);
            when(repo.existsByBookingId(event.bookingId())).thenReturn(false);
            when(clock.today()).thenReturn(TODAY);

            ledger.recordConfirmed(event);

            verify(repo).save(saved.capture());
            assertThat(saved.getValue().getCommission()).isEqualByComparingTo(commission);
            assertThat(saved.getValue().getHostAmount()).isEqualByComparingTo(host);
        }

        @Test
        void commissionPercent_isTakenFromConfigAndStoreOnTheRow() {
            ReflectionTestUtils.setField(ledger, "commissionPercent", new BigDecimal("12.5"));
            BookingConfirmedEvent event = confirmedEvent(UUID.randomUUID(), "4000.00");
            when(repo.existsByBookingId(event.bookingId())).thenReturn(false);

            ledger.recordConfirmed(event);

            verify(repo).save(saved.capture());
            assertThat(saved.getValue().getCommissionPercent()).isEqualByComparingTo("12.5");
            assertThat(saved.getValue().getCommission()).isEqualByComparingTo("500.00");
        }

        @Test
        void sameEventTwice_createNoSecondRow() {
            // idempotency (074): the row already exists for this booking
            BookingConfirmedEvent event = confirmedEvent(UUID.randomUUID(), "4000.00");
            when(repo.existsByBookingId(event.bookingId())).thenReturn(true);

            ledger.recordConfirmed(event);

            verify(repo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("recordCancelled")
    class RecordCancelled {

        private final UUID bookingId = UUID.randomUUID();

        @Test
        void unpaidBookingCancelled_touchesNothing() {
            BookingCancelledEvent event = cancelledEvent(false, null);

            ledger.recordCancelled(event);

            verifyNoInteractions(repo);
        }

        @Test
        void noRowForTheBooking_savesNothing() {
            // refund amount left out: the ledger stops before reading it (Mockito's strict stubs would complain)

            BookingCancelledEvent event = cancelledEvent(true, null);
            when(repo.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.empty());

            ledger.recordCancelled(event);

            verify(repo, never()).save(any());
        }

        @Test
        void alreadyReversed_isLeftAlone() {
            EarningsEntry entry = pendingEntry();
            entry.setStatus(EarningsStatus.REVERSED);
            BookingCancelledEvent event = cancelledEvent(true, null);
            when(repo.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.empty());

            ledger.recordCancelled(event);

            verify(repo, never()).save(any());
        }

        @Test
        void fullRefund_reversesTheRowAndHostGetsNothing() {
            EarningsEntry entry = pendingEntry();
            when(repo.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(entry));

            ledger.recordCancelled(cancelledEvent(true, "4000.00"));

            verify(repo).save(entry);
            assertThat(entry.getRefunded()).isEqualByComparingTo("4000.00");
            assertThat(entry.getCommission()).isEqualByComparingTo("0.00");
            assertThat(entry.getHostAmount()).isEqualByComparingTo("0.00");
            assertThat(entry.getStatus()).isEqualTo(EarningsStatus.REVERSED);
        }

        @Test
        void partialRefund_recalculatesOnWhatIsLeft_andStayPending() {
            EarningsEntry entry = pendingEntry();
            when(repo.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(entry));

            ledger.recordCancelled(cancelledEvent(true, "1000.00"));

            // base 3000 -> commission 300, host 2700
            assertThat(entry.getRefunded()).isEqualByComparingTo("1000.00");
            assertThat(entry.getCommission()).isEqualByComparingTo("300.00");
            assertThat(entry.getHostAmount()).isEqualByComparingTo("2700.00");
            assertThat(entry.getStatus()).isEqualTo(EarningsStatus.PENDING);
        }

        @Test
        void refundBiggerThanGross_isCappedAtGross() {
            EarningsEntry entry = pendingEntry();
            when(repo.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(entry));

            ledger.recordCancelled(cancelledEvent(true, "4000.00"));

            assertThat(entry.getRefunded()).isEqualByComparingTo("4000.00");
            assertThat(entry.getStatus()).isEqualTo(EarningsStatus.REVERSED);
        }

        @Test
        void paidButNotRefund_lateGuestCancel_hostKeepsFullAmount() {
            // guest cancelled after the 48 h window: refund is null, the host still earns
            EarningsEntry entry = pendingEntry();
            when(repo.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(entry));

            ledger.recordCancelled(cancelledEvent(true, null));

            assertThat(entry.getRefunded()).isEqualByComparingTo("0");
            assertThat(entry.getHostAmount()).isEqualByComparingTo("3600.00");
            assertThat(entry.getStatus()).isEqualTo(EarningsStatus.PENDING);
        }

        private EarningsEntry pendingEntry() {
            return EarningsEntry.builder()
                    .bookingId(bookingId).bookingReference("TEARN0001")
                    .gross(new BigDecimal("4000.00")).refunded(BigDecimal.ZERO)
                    .commissionPercent(new BigDecimal("10"))
                    .commission(new BigDecimal("400.00"))
                    .hostAmount(new BigDecimal("3600.00"))
                    .status(EarningsStatus.PENDING)
                    .currency("INR")
                    .build();
        }

        /**
         * BookingCancelledEvent is a record from the payment side; a Mockito mock lets us set only
         * the three values the ledger reads. (With its constructor we could build a real one.)
         */
        private BookingCancelledEvent cancelledEvent(boolean wasPaid, String refund) {
            BookingCancelledEvent event = mock(BookingCancelledEvent.class);
            when(event.wasPaid()).thenReturn(wasPaid);
            if (wasPaid) {
                when(event.bookingId()).thenReturn(bookingId);
            }
            if (wasPaid && refund != null) {
                when(event.refundAmount()).thenReturn(new BigDecimal(refund));
            }
            return event;
        }
    }

    private static BookingConfirmedEvent confirmedEvent(UUID bookingId, String total) {
        return new BookingConfirmedEvent(bookingId, "TBOOK0001", UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Test listing", "DOUBLE", LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12), 2, 1, 2,
                new BigDecimal(total), "INR");
    }
}