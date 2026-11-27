package com.blinkstay.earnings.repository;

import com.blinkstay.earnings.entities.EarningsEntry;
import com.blinkstay.earnings.enums.EarningsStatus;
import com.blinkstay.support.RepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static com.blinkstay.earnings.enums.EarningsStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
@DisplayName("EarningsRepository on real Postgres")
class EarningsRepositoryIT {

    private static final Instant NOW = Instant.parse("2030-01-15T06:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2030, 1, 15);

    @Autowired
    private EarningsRepository repo;

    private final UUID hostId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    // ---------- helpers ----------

    private static LocalDate d(int month, int day) {
        return LocalDate.of(2030, month, day);
    }

    /**
     * A valid, unsaved row: 4000 gross, 10 % commission, sold 5 days before check-in.
     */
    private EarningsEntry newEntry(EarningsStatus status, LocalDate checkIn, LocalDate checkOut) {
        return EarningsEntry.builder()
                .bookingId(UUID.randomUUID()).bookingReference("T123456789")
                .hostId(hostId).listingId(listingId).listingTitle("Test listing")
                .checkIn(checkIn).checkOut(checkOut).currency("INR")
                .gross(new BigDecimal("4000.00")).refunded(new BigDecimal("0.00"))
                .commissionPercent(new BigDecimal("10.00")).commission(new BigDecimal("400.00"))
                .hostAmount(new BigDecimal("3600.00"))
                .status(status)
                .soldOn(checkIn.minusDays(5))
                .build();
    }

    private EarningsEntry saved(EarningsStatus status, LocalDate checkIn, LocalDate checkOut) {
        return repo.saveAndFlush(newEntry(status, checkIn, checkOut));
    }

    private EarningsEntry reload(EarningsEntry entry) {
        return repo.findById(entry.getId()).orElseThrow();
    }

    // ---------- job and ledger ----------

    @Test
    @DisplayName("markEarned: only PENDING rows whose stay ended before today become EARNED")
    void markEarned_earnsOnlyPendingStaysThatEndedBeforeToday() {
        EarningsEntry ended = saved(PENDING, d(1, 12), d(1, 14));
        EarningsEntry leavingToday = saved(PENDING, d(1, 13), TODAY);
        EarningsEntry reversed = saved(REVERSED, d(1, 12), d(1, 14));

        int changed = repo.markEarned(TODAY, NOW);

        assertThat(changed).isEqualTo(1);
        EarningsEntry earned = reload(ended);
        assertThat(earned.getStatus()).isEqualTo(EARNED);
        assertThat(earned.getEarnedAt()).isEqualTo(NOW);
        assertThat(earned.getVersion()).isEqualTo(1L);
        assertThat(reload(leavingToday).getStatus()).isEqualTo(PENDING);
        assertThat(reload(reversed).getStatus()).isEqualTo(REVERSED);
    }

    @Test
    void existsByBookingId_isTrueOnlyForAStoredBooking() {
        EarningsEntry entry = saved(PENDING, d(1, 20), d(1, 22));

        assertThat(repo.existsByBookingId(entry.getBookingId())).isTrue();
        assertThat(repo.existsByBookingId(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("a second row for the same booking is rejected (one ledger row per booking)")
    void secondRowForTheSameBooking_isRejectedByTheDatabase() {
        EarningsEntry first = saved(PENDING, d(1, 20), d(1, 22));
        EarningsEntry copy = newEntry(PENDING, d(1, 20), d(1, 22));
        copy.setBookingId(first.getBookingId());

        assertThatThrownBy(() -> repo.saveAndFlush(copy))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByBookingIdForUpdate_returnsTheRow() {
        EarningsEntry entry = saved(PENDING, d(1, 20), d(1, 22));

        assertThat(repo.findByBookingIdForUpdate(entry.getBookingId()))
                .map(EarningsEntry::getId).contains(entry.getId());
    }

    // ---------- host reports (filter on checkIn) ----------

    @Nested
    @DisplayName("host reports")
    class HostReports {

        private final LocalDate from = d(1, 1);
        private final LocalDate to = d(3, 31);

        @Test
        @DisplayName("totalsByStatus: only this host, this currency and check-ins inside the range")
        void totalsByStatus_filtersHostCurrencyAndDates() {
            saved(PENDING, d(1, 20), d(1, 22));                       // the only row that counts
            EarningsEntry usd = newEntry(EARNED, d(1, 8), d(1, 9));
            usd.setCurrency("USD");
            repo.saveAndFlush(usd);
            EarningsEntry otherHost = newEntry(REVERSED, d(1, 8), d(1, 9));
            otherHost.setHostId(UUID.randomUUID());
            repo.saveAndFlush(otherHost);
            saved(EARNED, d(4, 2), d(4, 4));                           // after the range

            assertThat(repo.totalsByStatus(hostId, from, to, "INR")).hasSize(1);
        }

        @Test
        @DisplayName("totalsByStatus: one row per status, REVERSED included")
        void totalsByStatus_returnsOneRowPerStatus() {
            saved(PENDING, d(1, 20), d(1, 22));
            saved(EARNED, d(1, 5), d(1, 7));
            saved(EARNED, d(2, 5), d(2, 7));
            saved(REVERSED, d(2, 10), d(2, 12));

            assertThat(repo.totalsByStatus(hostId, from, to, "INR")).hasSize(3);
        }

        @Test
        @DisplayName("between is inclusive: check-ins on the first and the last day both count")
        void dateRange_includesBothEnds() {
            saved(PENDING, from, from.plusDays(1));
            saved(EARNED, to, to.plusDays(1));

            assertThat(repo.totalsByStatus(hostId, from, to, "INR")).hasSize(2);
        }

        @Test
        void byListing_skipsListingsThatOnlyHaveReversedRows() {
            saved(EARNED, d(1, 5), d(1, 7));
            EarningsEntry secondListing = newEntry(PENDING, d(1, 20), d(1, 22));
            secondListing.setListingId(UUID.randomUUID());
            repo.saveAndFlush(secondListing);
            EarningsEntry reversedListing = newEntry(REVERSED, d(1, 20), d(1, 22));
            reversedListing.setListingId(UUID.randomUUID());
            repo.saveAndFlush(reversedListing);

            assertThat(repo.byListing(hostId, from, to, "INR")).hasSize(2);
        }

        @Test
        void byMonth_groupsByCheckInMonth_andSkipsReversed() {
            saved(EARNED, d(1, 5), d(1, 7));
            saved(PENDING, d(1, 20), d(1, 22));
            saved(EARNED, d(2, 5), d(2, 7));
            saved(REVERSED, d(3, 5), d(3, 7)); // March has only a reversed row

            assertThat(repo.byMonth(hostId, from, to, "INR")).hasSize(2);
        }
    }

    // ---------- admin reports (filter on soldOn) ----------

    @Test
    @DisplayName("platformMonthly: groups by the sold-on month; rows without soldOn are left out")
    void platformMonthly_groupsBySoldOnMonth() {
        EarningsEntry january = newEntry(EARNED, d(2, 1), d(2, 3));
        january.setSoldOn(d(1, 10));
        repo.saveAndFlush(january);
        EarningsEntry february = newEntry(PENDING, d(3, 1), d(3, 3));
        february.setSoldOn(d(2, 10));
        repo.saveAndFlush(february);
        EarningsEntry noSoldOn = newEntry(PENDING, d(3, 5), d(3, 7));
        noSoldOn.setSoldOn(null);
        repo.saveAndFlush(noSoldOn);

        assertThat(repo.platformMonthly(d(1, 1), d(12, 31), "INR")).hasSize(2);
    }

    @Test
    void topHosts_returnsAtMostThePageSize() {
        for (int i = 0; i < 3; i++) {
            EarningsEntry entry = newEntry(EARNED, d(2, 1), d(2, 3));
            entry.setHostId(UUID.randomUUID());
            repo.saveAndFlush(entry);
        }

        assertThat(repo.topHosts(d(1, 1), d(12, 31), "INR", PageRequest.of(0, 2))).hasSize(2);
    }
}