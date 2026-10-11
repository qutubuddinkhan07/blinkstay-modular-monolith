package blinkstay.auth.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.auth.dtos.AdminHostRequestDto;
import blinkstay.auth.dtos.ApiResponse;
import blinkstay.auth.dtos.PageResult;
import blinkstay.auth.dtos.RejectRequestDto;
import blinkstay.auth.enums.RequestStatus;
import blinkstay.auth.service.HostRequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/admin/host-requests")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminHostRequestController {
	private final HostRequestService hostRequestService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResult<AdminHostRequestDto>>> list(
			@RequestParam(required = false) RequestStatus status, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity
				.ok(ApiResponse.of(true, "Host requests", hostRequestService.listRequests(status, page, size)));
	}

	@GetMapping("/pending-count")
	public ResponseEntity<ApiResponse<Long>> pendingCount() {
		return ResponseEntity.ok(ApiResponse.of(true, "Pending host requests", hostRequestService.countPending()));
	}

	@PostMapping("/{requestId}/approve")
	public ResponseEntity<ApiResponse<String>> approve(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable("requestId") UUID requestId) {
		hostRequestService.approve(UUID.fromString(userDetails.getUsername()), requestId);
		return ResponseEntity.ok(ApiResponse.of(true, "Request approved", null));
	}

	@PostMapping("/{requestId}/reject")
	public ResponseEntity<ApiResponse<String>> reject(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable("requestId") UUID requestId, @Valid @RequestBody RejectRequestDto dto) {
		hostRequestService.reject(UUID.fromString(userDetails.getUsername()), requestId, dto.getReason());
		return ResponseEntity.ok(ApiResponse.of(true, "Request rejected", null));
	}
}
