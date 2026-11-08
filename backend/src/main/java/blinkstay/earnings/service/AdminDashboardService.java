package blinkstay.earnings.service;

import blinkstay.earnings.dto.AdminOverviewDto;
import blinkstay.earnings.dto.ListingEarningsDto;
import blinkstay.earnings.dto.PlatformMonthlyDto;
import blinkstay.earnings.dto.TopHostDto;

import java.time.LocalDate;
import java.util.List;

public interface AdminDashboardService {
    AdminOverviewDto overview(LocalDate from, LocalDate to);

    List<PlatformMonthlyDto> monthly(LocalDate from, LocalDate to);

    List<TopHostDto> topHosts(LocalDate from, LocalDate to, int limit);

    List<ListingEarningsDto> topListings(LocalDate from, LocalDate to, int limit);
}
