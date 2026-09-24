package blinkstay.listing.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import blinkstay.listing.dto.AddListingDto;
import blinkstay.listing.dto.ImageDto;
import blinkstay.listing.dto.ListingApiResponse;
import blinkstay.listing.dto.ListingDetailsResponseDto;
import blinkstay.listing.dto.PublishedListingDto;
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
@RequiredArgsConstructor
@Validated
public class ListingController {
	private final ListingService listingService;

//	@GetMapping("/published")
	@GetMapping("/all")
	public ResponseEntity<Page<PublishedListingDto>> getPublishedListings(

			@RequestParam(defaultValue = "0") int page,

			@RequestParam(defaultValue = "10") int size,

			@RequestParam(defaultValue = "createdAt") String sortBy,

			@RequestParam(defaultValue = "desc") String direction,

			@RequestParam(required = false) String country) {

		return ResponseEntity.ok(listingService.getPublishedListings(page, size, sortBy, direction, country));
	}

	@Operation(summary = "Create a new listing with images")
	@SecurityRequirement(name = "Bearer Authentication")
	@PreAuthorize("hasAnyAuthority('HOTEL_MANAGER','ADMIN')")
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

	@Operation(summary = "Publish listing")
	@SecurityRequirement(name = "Bearer Authentication")
	@PreAuthorize("hasAnyAuthority('HOTEL_MANAGER','ADMIN')")
	@PostMapping("/{listingId}/publish")
	public ResponseEntity<ListingApiResponse<String>> publishListing(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable UUID listingId) {

		UUID userId = UUID.fromString(userDetails.getUsername());

		String response = listingService.listingPublishService(userId, listingId);

		ListingApiResponse<String> apiResponse = ListingApiResponse.<String>builder().success(true)
				.message("Listing published successfully").data(response).build();

		return ResponseEntity.ok(apiResponse);
	}

	@GetMapping("/my-listings")
	@SecurityRequirement(name = "Bearer Authentication")
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

	@SecurityRequirement(name = "Bearer Authentication")
	@PreAuthorize("hasAuthority('ADMIN')")
	@GetMapping("/manager/{managerId}")
	public ResponseEntity<ListingApiResponse<List<ListingDetailsResponseDto>>> getAllListingIds(
			@PathVariable("managerId") UUID userId) {

		List<ListingDetailsResponseDto> serviceResponse = listingService.getAllListingsByManager(userId);
		ListingApiResponse<List<ListingDetailsResponseDto>> apiResponse = ListingApiResponse
				.<List<ListingDetailsResponseDto>>builder().success(true)
				.message("Returning all listing IDs owned by manager").data(serviceResponse).build();

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Update listing")
	@SecurityRequirement(name = "Bearer Authentication")
	@PreAuthorize("hasAnyAuthority('HOTEL_MANAGER','ADMIN')")
	@PutMapping("/{listingId}")
	public ResponseEntity<ListingApiResponse<String>> updateListing(@AuthenticationPrincipal UserDetails userDetails,

			@Parameter(description = "Listing ID") @PathVariable UUID listingId,

			@Parameter(description = "Updated listing details") @Valid @RequestBody AddListingDto addListingDto) {

		UUID userId = UUID.fromString(userDetails.getUsername());

		String response = listingService.updateListing(userId, listingId, addListingDto);

		ListingApiResponse<String> apiResponse = ListingApiResponse.<String>builder().success(true)
				.message("Listing updated successfully").data(response).build();

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Add images to listing")
	@SecurityRequirement(name = "Bearer Authentication")
	@PreAuthorize("hasAnyAuthority('HOTEL_MANAGER','ADMIN')")
	@PostMapping(value = "/{listingId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ListingApiResponse<List<ImageDto>>> addListingImages(
			@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID listingId,
			@NotEmpty(message = "At list one image is required") @RequestPart("images") List<MultipartFile> images) {
		if (images == null || images.isEmpty() || images.stream().allMatch(MultipartFile::isEmpty)) {
			throw new IllegalArgumentException("At least one image file must be uploaded.");
		}

		UUID userId = UUID.fromString(userDetails.getUsername());

		List<ImageDto> uploadedImages = listingService.addListingImages(userId, listingId, images);

		ListingApiResponse<List<ImageDto>> response = ListingApiResponse.<List<ImageDto>>builder().success(true)
				.message("Images added successfully").data(uploadedImages).build();

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Delete listing image")
	@SecurityRequirement(name = "Bearer Authentication")
	@PreAuthorize("hasAnyAuthority('HOTEL_MANAGER','ADMIN')")
	@DeleteMapping("/{listingId}/images/{imageId}")
	public ResponseEntity<ListingApiResponse<String>> deleteListingImage(
			@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID listingId,
			@PathVariable UUID imageId) {

		UUID userId = UUID.fromString(userDetails.getUsername());

		listingService.deleteListingImage(userId, listingId, imageId);

		ListingApiResponse<String> response = ListingApiResponse.<String>builder().success(true)
				.message("Image deleted successfully").data("Image deleted").build();

		return ResponseEntity.ok(response);
	}
}
