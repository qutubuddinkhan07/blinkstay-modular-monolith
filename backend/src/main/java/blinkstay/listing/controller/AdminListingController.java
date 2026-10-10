package blinkstay.listing.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.listing.dto.ListingApiResponse;
import blinkstay.listing.dto.SuspendListingDto;
import blinkstay.listing.service.ListingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v3/admin/listings")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminListingController {
	private final ListingService listingService;

	@PostMapping("/{listingId}/suspend")
	public ResponseEntity<ListingApiResponse<String>> suspend(@PathVariable UUID listingId,
			@Valid @RequestBody SuspendListingDto dto) {
		String msg = listingService.suspendListing(listingId, dto.getReason());
		return ResponseEntity.ok(ListingApiResponse.<String>builder().success(true).message(msg).data(null).build());
	}

	@PostMapping("/{listingId}/unsuspend")
	public ResponseEntity<ListingApiResponse<String>> unsuspend(@PathVariable UUID listingId) {
		String msg = listingService.unsuspendListing(listingId);
		return ResponseEntity.ok(ListingApiResponse.<String>builder().success(true).message(msg).data(null).build());
	}
}
