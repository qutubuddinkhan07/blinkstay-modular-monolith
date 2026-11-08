package blinkstay.earnings.controller;

import blinkstay.earnings.dto.*;
import blinkstay.earnings.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v5/admin/dashboard")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminDashboardController {
    private final AdminDashboardService service;

    // dates are the day the booking was confirmed: ?from=2026-01-01&to=2026-12-31
    @GetMapping("/overview")
    public ResponseEntity<EarningsApiResponse<AdminOverviewDto>> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(EarningsApiResponse.success("Platform overview", service.overview(from, to)));
    }

    @GetMapping("/monthly")
    public ResponseEntity<EarningsApiResponse<List<PlatformMonthlyDto>>> monthly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(EarningsApiResponse.success("Monthly platform sales", service.monthly(from, to)));
    }

    @GetMapping("/top-hosts")
    public ResponseEntity<EarningsApiResponse<List<TopHostDto>>> topHosts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(EarningsApiResponse.success("Top hosts", service.topHosts(from, to, limit)));
    }

    @GetMapping("/top-listings")
    public ResponseEntity<EarningsApiResponse<List<ListingEarningsDto>>> topListings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(EarningsApiResponse.success("Top listings", service.topListings(from, to, limit)));
    }
}