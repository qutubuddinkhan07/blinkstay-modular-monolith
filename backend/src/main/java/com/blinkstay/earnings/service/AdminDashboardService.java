package com.blinkstay.earnings.service;

import com.blinkstay.earnings.dto.AdminOverviewDto;
import com.blinkstay.earnings.dto.ListingEarningsDto;
import com.blinkstay.earnings.dto.PlatformMonthlyDto;
import com.blinkstay.earnings.dto.TopHostDto;

import java.time.LocalDate;
import java.util.List;

public interface AdminDashboardService {
    AdminOverviewDto overview(LocalDate from, LocalDate to);

    List<PlatformMonthlyDto> monthly(LocalDate from, LocalDate to);

    List<TopHostDto> topHosts(LocalDate from, LocalDate to, int limit);

    List<ListingEarningsDto> topListings(LocalDate from, LocalDate to, int limit);
}
