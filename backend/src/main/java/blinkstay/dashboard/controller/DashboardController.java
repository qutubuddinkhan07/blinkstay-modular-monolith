package blinkstay.dashboard.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.dashboard.dto.ManagerDashboardDto;
import blinkstay.dashboard.service.ManagerDashboardService;
import blinkstay.listing.dto.ListingApiResponse;

@RestController
public class DashboardController {
	private ManagerDashboardService managerDashboardService;

	@GetMapping("/my-dashboard")
	@PreAuthorize("hasAuthority('HOTEL_MANAGER')")
	public ResponseEntity<ListingApiResponse<ManagerDashboardDto>> getManagerDashboard(
			@AuthenticationPrincipal UserDetails userDetails) {

		UUID managerId = UUID.fromString(userDetails.getUsername());

		ManagerDashboardDto dashboard = managerDashboardService.getManagerDashboard(managerId);

		ListingApiResponse<ManagerDashboardDto> response = ListingApiResponse.<ManagerDashboardDto>builder()
				.success(true).message("Manager dashboard fetched successfully").data(dashboard).build();

		return ResponseEntity.ok(response);
	}
}
