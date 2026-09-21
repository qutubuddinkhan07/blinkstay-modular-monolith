package blinkstay.listing.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v3/listings")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_MANAGER')")
public class ListingController {
//	public ResponseEntity<ListingApiResponse<String>> createListing(@AuthenticationPrincipal UserDetails userDetails, )
}
