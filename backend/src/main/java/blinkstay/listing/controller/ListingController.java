package blinkstay.listing.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import blinkstay.listing.dto.AddListingDto;
import blinkstay.listing.dto.ListingApiResponse;
import blinkstay.listing.dto.ListingDetailsResponseDto;
import blinkstay.listing.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v3/listings")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAuthority('HOTEL_MANAGER')")
@RequiredArgsConstructor
@Validated
public class ListingController {
	private final ListingService listingService;

	@Operation(summary = "Create a new listing with images")
	@PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ListingApiResponse<String>> createListing(@AuthenticationPrincipal UserDetails userDetails,
			@Parameter(description = "Listing details in JSON format", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AddListingDto.class))) @Valid @RequestPart("listing") AddListingDto addListingDto,
			@NotEmpty(message = "At least one image is required") @RequestPart("images") List<MultipartFile> images) {

		// Validate that at least 1 image for (or minimum required) is provided
		if (images == null || images.isEmpty() || images.stream().allMatch(MultipartFile::isEmpty)) {
			throw new IllegalArgumentException("At least one image file must be uploaded.");
		}

		UUID userId = UUID.fromString(userDetails.getUsername());

		String listingId = listingService.createListing(userId, addListingDto, images);
		ListingApiResponse<String> response = ListingApiResponse.<String>builder().success(true)
				.message("Listing created successfully").data(listingId).build();

		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	@GetMapping("/{listingId}")
	public ResponseEntity<ListingApiResponse<ListingDetailsResponseDto>> getListingByIdController(
			@PathVariable("listingId") UUID listingId) {
		ListingDetailsResponseDto listingDetailsResponseDto = listingService.getListingById(listingId);
		ListingApiResponse<ListingDetailsResponseDto> response = ListingApiResponse.<ListingDetailsResponseDto>builder()
				.success(true).message("Listing fetched successfully").data(listingDetailsResponseDto).build();

		return ResponseEntity.ok(response);
	}

	@GetMapping("/my-listings")
	@PreAuthorize("hasAuthority('HOTEL_MANAGER')")
	public ResponseEntity<ListingApiResponse<List<ListingDetailsResponseDto>>> getAllListingIds(
			@AuthenticationPrincipal UserDetails userDetails) {
		UUID managerId = UUID.fromString(userDetails.getUsername());

		List<ListingDetailsResponseDto> serviceResponse = listingService.getAllListingsByManager(managerId);

		ListingApiResponse<List<ListingDetailsResponseDto>> apiResponse = ListingApiResponse
				.<List<ListingDetailsResponseDto>>builder().success(true)
				.message("Returning all listings owned by manager").data(serviceResponse).build();

		return ResponseEntity.ok(apiResponse);
	}

	@GetMapping("/manager/{managerId}")
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<ListingApiResponse<List<ListingDetailsResponseDto>>> getAllListingIds(
			@PathVariable("managerId") UUID userId) {

		List<ListingDetailsResponseDto> serviceResponse = listingService.getAllListingsByManager(userId);
		ListingApiResponse<List<ListingDetailsResponseDto>> apiResponse = ListingApiResponse
				.<List<ListingDetailsResponseDto>>builder().success(true)
				.message("Returning all listing IDs owned by manager").data(serviceResponse).build();

		return ResponseEntity.ok(apiResponse);
	}
}
