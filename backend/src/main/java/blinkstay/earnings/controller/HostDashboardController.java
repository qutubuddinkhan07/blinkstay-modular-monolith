package blinkstay.earnings.controller;

import blinkstay.common.dto.PageResult;
import blinkstay.earnings.dto.*;
import blinkstay.earnings.enums.EarningsStatus;
import blinkstay.earnings.service.HostDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v5/host/dashboard")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('HOTEL_MANAGER')")
public class HostDashboardController {
    private final HostDashboardService service;

    // dates are check-in dates: ?from=2026-01-01&to=2026-12-31
    @GetMapping("/summary")
    public ResponseEntity<EarningsApiResponse<EarningsSummaryDto>> summary(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(EarningsApiResponse.success("Earnings summary",
                service.summary(UUID.fromString(ud.getUsername()), from, to)));
    }

    @GetMapping("/by-listing")
    public ResponseEntity<EarningsApiResponse<List<ListingEarningsDto>>> byListing(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(EarningsApiResponse.success("Earnings per listing",
                service.byListing(UUID.fromString(ud.getUsername()), from, to)));
    }

    @GetMapping("/monthly")
    public ResponseEntity<EarningsApiResponse<List<MonthlyEarningsDto>>> monthly(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(EarningsApiResponse.success("Monthly earnings",
                service.monthly(UUID.fromString(ud.getUsername()), from, to)));
    }

    // e.g. ?status=EARNED&page=0&size=10
    @GetMapping("/ledger")
    public ResponseEntity<EarningsApiResponse<PageResult<LedgerEntryDto>>> ledger(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) EarningsStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(EarningsApiResponse.success("Earnings ledger",
                service.ledger(UUID.fromString(ud.getUsername()), status, from, to, page, size)));
    }
}