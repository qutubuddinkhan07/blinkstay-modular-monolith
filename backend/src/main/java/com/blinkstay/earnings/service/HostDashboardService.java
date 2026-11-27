package com.blinkstay.earnings.service;

import com.blinkstay.common.dto.PageResult;
import com.blinkstay.earnings.dto.EarningsSummaryDto;
import com.blinkstay.earnings.dto.LedgerEntryDto;
import com.blinkstay.earnings.dto.ListingEarningsDto;
import com.blinkstay.earnings.dto.MonthlyEarningsDto;
import com.blinkstay.earnings.enums.EarningsStatus;

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
