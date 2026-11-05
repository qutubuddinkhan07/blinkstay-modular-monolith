package blinkstay.earnings.service;

import blinkstay.common.dto.PageResult;
import blinkstay.earnings.dto.EarningsSummaryDto;
import blinkstay.earnings.dto.LedgerEntryDto;
import blinkstay.earnings.dto.ListingEarningsDto;
import blinkstay.earnings.dto.MonthlyEarningsDto;
import blinkstay.earnings.enums.EarningsStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HostDashboardService {
    EarningsSummaryDto summary(UUID hostId, LocalDate from, LocalDate to);

    List<ListingEarningsDto> byListing(UUID hostId, LocalDate from, LocalDate to);

    List<MonthlyEarningsDto> monthly(UUID hostId, LocalDate from, LocalDate to);

    PageResult<LedgerEntryDto> ledger(UUID hostId, EarningsStatus status, LocalDate from, LocalDate to, int page,
                                      int size);
}
