package blinkstay.listing.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import blinkstay.listing.dto.AddListingDto;
import blinkstay.listing.dto.ImageDto;
import blinkstay.listing.dto.ListingDetailsResponseDto;
import blinkstay.listing.dto.PublishedListingDto;
import blinkstay.listing.entities.Listing;

public interface ListingService {
	String createListing(UUID managerId, AddListingDto addListingDto, List<MultipartFile> images);

	ListingDetailsResponseDto getListingById(UUID listingId);

	List<ListingDetailsResponseDto> getAllListingsByManager(UUID managerId);

	List<Listing> getListingsByManagerId(UUID managerId);

	// for checking from room service
	boolean checkWhetherSameManager(UUID userId, UUID listingId);

	String listingPublishService(UUID userId, UUID listingId);

	String updateListing(UUID userId, UUID listingId, AddListingDto dto);

	List<ImageDto> addListingImages(UUID userId, UUID listingId, List<MultipartFile> files);

	void deleteListingImage(UUID userId, UUID listingId, UUID imageId);

	Page<PublishedListingDto> getPublishedListings(int page, int size, String sortBy, String direction, String country);
}
