package blinkstay.listing.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.listing.dto.ListingApiResponse;
import blinkstay.listing.dto.PagedResponse;
import blinkstay.listing.dto.PublishedListingDto;
import blinkstay.listing.service.FavoriteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v3/favorites")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class FavoriteController {
	private final FavoriteService favoriteService;

	@GetMapping
	public ResponseEntity<PagedResponse<PublishedListingDto>> list(@AuthenticationPrincipal UserDetails ud,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
		return ResponseEntity.ok(favoriteService.list(UUID.fromString(ud.getUsername()), page, size));
	}

	@GetMapping("/ids")
	public ResponseEntity<ListingApiResponse<List<UUID>>> ids(@AuthenticationPrincipal UserDetails ud) {
		return ResponseEntity.ok(ListingApiResponse.<List<UUID>>builder().success(true).message("Favorite listing IDs")
				.data(favoriteService.ids(UUID.fromString(ud.getUsername()))).build());
	}

	@PutMapping("/{listingId}")
	public ResponseEntity<ListingApiResponse<String>> add(@AuthenticationPrincipal UserDetails ud,
			@PathVariable("listingId") UUID listingId) {
		favoriteService.add(UUID.fromString(ud.getUsername()), listingId);
		return ResponseEntity.ok(
				ListingApiResponse.<String>builder().success(true).message("Added to favorites").data(null).build());
	}

	@DeleteMapping("/{listingId}")
	public ResponseEntity<ListingApiResponse<String>> remove(@AuthenticationPrincipal UserDetails ud,
			@PathVariable("listingId") UUID listingId) {
		favoriteService.remove(UUID.fromString(ud.getUsername()), listingId);
		return ResponseEntity.ok(ListingApiResponse.<String>builder().success(true).message("Removed from favorites")
				.data(null).build());
	}
}
