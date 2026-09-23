package blinkstay.listing.serviceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.common.exception.ManagerNotOwnerException;
import blinkstay.listing.dto.AddListingDto;
import blinkstay.listing.dto.GeocodingResult;
import blinkstay.listing.dto.ImageDto;
import blinkstay.listing.dto.ImageUploadResult;
import blinkstay.listing.dto.ListingDetailsResponseDto;
import blinkstay.listing.entities.Listing;
import blinkstay.listing.entities.ListingGeometry;
import blinkstay.listing.entities.ListingImage;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.listing.mapper.ModelMapper;
import blinkstay.listing.repository.ListingGeometryRepository;
import blinkstay.listing.repository.ListingImageRepository;
import blinkstay.listing.repository.ListingRepository;
import blinkstay.listing.service.ImageUploadService;
import blinkstay.listing.service.ListingService;
import blinkstay.listing.service.MapboxGeocodingService;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.dto.RoomSummaryDto;
import blinkstay.room.service.RoomService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingServiceImpl implements ListingService {
	@Qualifier("lisitingImageUploadService")
	private final ImageUploadService imageUploadService;

	private final ListingRepository listingRepo;

	private final ListingImageRepository listingImageRepo;

	private final ListingGeometryRepository listingGeometryRepo;

	private final MapboxGeocodingService mapboxGeocodingService;

	@Qualifier("listingModelMapper")
	private final ModelMapper modelMapper;

	// contacting room service
	private final RoomService roomService;

	/**
	 * Helper method to clean up orphan Cloudinary uploads
	 */
	private void rollbackUploadedImages(List<String> publicIds) {
		for (String publicId : publicIds) {
			try {
				log.info("Rolling back Cloudinary image: {}", publicId);
				imageUploadService.deleteImage(publicId);
			} catch (Exception e) {
				log.error("Failed to delete image from Cloudinary during rollback for publicId: {}", publicId, e);
			}
		}
	}

	// Helper methods
	private Listing helperGetListingById(UUID listingId) {
		Listing listing = listingRepo.findById(listingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No listing present with this id: " + listingId));

		return listing;
	}

	/**
	 * ---------------------------------------------------
	 */

	@Override
	@Transactional
	public String createListing(UUID managerId, AddListingDto addListingDto, List<MultipartFile> files) {
		log.info("Creating listing for user: {}", managerId);

		// Track successfully uploaded public IDs for potential cleanup
		List<String> uploadedPublicIds = new ArrayList<>();

		try {
			// 1. Create Listing
			Listing listing = modelMapper.addListingDtoToListing(addListingDto, managerId);
			Listing savedListing = listingRepo.save(listing);

			List<ListingImage> imageEntities = new ArrayList<>();

			// 2. Get coordinates from Mapbox
			GeocodingResult geocodingResult = mapboxGeocodingService.getCoordinates(addListingDto.getLocation(),
					addListingDto.getCountry());

			// 3. Saving ListingGeometry
			ListingGeometry geometry = ListingGeometry.builder().listingId(savedListing.getId())
					.address(geocodingResult.getAddress()).longitude(geocodingResult.getLongitude())
					.latitude(geocodingResult.getLatitude()).build();

			listingGeometryRepo.save(geometry);

			// 4. Uploading images
			for (int i = 0; i < files.size(); i++) {
				MultipartFile file = files.get(i);
				if (file == null || file.isEmpty()) {
					continue;
				}

				// Generate unique public_id (fixed space in string formatting)
				String publicId = "listing_" + savedListing.getId() + "_" + System.currentTimeMillis() + "_" + (i + 1);

				// Upload to Cloudinary
				ImageUploadResult uploadResult = imageUploadService.uploadImage(file, publicId);

				// Track uploaded ID immediately after success
				if (uploadResult != null && uploadResult.getPublicId() != null) {
					uploadedPublicIds.add(uploadResult.getPublicId());
				}

				ListingImage listingImage = modelMapper.imageUploadResultToListingImage(uploadResult,
						savedListing.getId(), i + 1);

				imageEntities.add(listingImage);
			}

			if (imageEntities.isEmpty()) {
				throw new IllegalArgumentException("At least one valid image is required.");
			}

			// 3. Save images to DB
			listingImageRepo.saveAll(imageEntities);

			return savedListing.getId().toString();

		} catch (Exception ex) {
			log.error("Failed to create listing for managerId {}. Triggering Cloudinary image cleanup.", managerId, ex);

			// Fallback cleanup logic
			rollbackUploadedImages(uploadedPublicIds);

			// Re-throw exception so @Transactional rolls back database changes
			throw ex;
		}
	}

	@Override
	public boolean checkWhetherSameManager(UUID userId, UUID listingId) {

		Listing listing = helperGetListingById(listingId);

		if (!userId.equals(listing.getManagerId())) {
			throw new ManagerNotOwnerException("You are not authorized to manage this listing");
		}

		return true;
	}

	@Override
	public ListingDetailsResponseDto getListingById(UUID listingId) {

		Listing listing = helperGetListingById(listingId);

		ListingGeometry geometry = listingGeometryRepo.findByListingId(listingId).orElse(null);

		List<ListingImage> images = listingImageRepo.findByListingIdOrderByDisplayOrderAsc(listingId);

		// Get complete room details
		List<RoomResponseDto> rooms = fetchRoomsDetails(listingId);

		ListingDetailsResponseDto response = modelMapper.listingDetailsMapper(listing, geometry, images);

		response.setRooms(rooms);

		return response;
	}

	@Override
	public List<ListingDetailsResponseDto> getAllListingsByManager(UUID managerId) {

		List<Listing> listings = listingRepo.findAllByManagerId(managerId);

		if (listings.isEmpty()) {
			return Collections.emptyList();
		}

		// Extract all listing IDs
		List<UUID> listingIds = listings.stream().map(Listing::getId).collect(Collectors.toList());

		// Batch fetch geometries
		Map<UUID, ListingGeometry> geometryMap = listingGeometryRepo.findByListingIdIn(listingIds).stream()
				.collect(Collectors.toMap(ListingGeometry::getListingId, Function.identity()));

		// Batch fetch images
		Map<UUID, List<ListingImage>> imagesMap = listingImageRepo.findByListingIdInOrderByDisplayOrderAsc(listingIds)
				.stream().collect(Collectors.groupingBy(ListingImage::getListingId));

		// Create response for each listing
		return listings.stream().map(listing -> {

			UUID listingId = listing.getId();

			ListingGeometry geometry = geometryMap.get(listingId);

			List<ListingImage> images = imagesMap.getOrDefault(listingId, Collections.emptyList());

			// Get room summary
			RoomSummaryDto roomSummary = roomService.getRoomSummary(listingId);

			// Map listing details
			ListingDetailsResponseDto response = modelMapper.listingDetailsMapper(listing, geometry, images);

			// Add room summary
			response.setRoomSummary(roomSummary);

			return response;
		}).collect(Collectors.toList());
	}

	private List<RoomResponseDto> fetchRoomsDetails(UUID listingId) {
		return roomService.getRoomsByListingId(listingId);
	}

	@Override
	public List<Listing> getListingsByManagerId(UUID managerId) {
		return listingRepo.findAllByManagerId(managerId);
	}

	@Override
	public String listingPublishService(UUID userId, UUID listingId) {
		checkWhetherSameManager(userId, listingId);

		Listing listing = listingRepo.findById(listingId)
				.orElseThrow(() -> new RuntimeException("No listing exist with this id"));

		if (!roomService.checkDoesListingHaveRooms(listingId)) {
			throw new RuntimeException("Listing cannot be published because it has no rooms");
		}

		if (listing.getStatus() == ListingStatus.PUBLISHED) {
			throw new RuntimeException("Listing already published");
		}

		listing.setStatus(ListingStatus.PUBLISHED);

		listingRepo.save(listing);

		return listing.getTitle() + " having id: " + listing.getId() + " PUBLISHED";
	}

	@Override
	public String updateListing(UUID managerId, UUID listingId, AddListingDto dto) {
		Listing listing = listingRepo.findById(listingId)
				.orElseThrow(() -> new RuntimeException("No listing exist with this id"));

		listing.setTitle(dto.getTitle());
		listing.setLocation(dto.getLocation());
		listing.setDescription(dto.getDescription());
		listing.setCountry(dto.getCountry());
		listing.setAmenities(dto.getAmenities());
		listing.setCategory(dto.getCategory());

		log.info("Updating listing for user: {}", managerId);

		try {
			// 1. Create Listing
			Listing savedListing = listingRepo.save(listing);

			// 2. Get coordinates from Mapbox
			GeocodingResult geocodingResult = mapboxGeocodingService.getCoordinates(dto.getLocation(),
					dto.getCountry());

			// 3. Saving ListingGeometry
			ListingGeometry geometry = ListingGeometry.builder().listingId(savedListing.getId())
					.address(geocodingResult.getAddress()).longitude(geocodingResult.getLongitude())
					.latitude(geocodingResult.getLatitude()).build();

			listingGeometryRepo.save(geometry);

			return savedListing.getId().toString();

		} catch (Exception ex) {
			log.error("Failed to update listing for managerId {}. Listing id {}.", managerId, listingId, ex);
			throw ex;
		}
	}

	@Override
	public List<ImageDto> addListingImages(UUID userId, UUID listingId, List<MultipartFile> files) {
		checkWhetherSameManager(userId, listingId);

		// to check whether the listing exists or not
		helperGetListingById(listingId);

		// Get current number of images
		int currentImageCount = listingImageRepo.countByListingId(listingId);

		List<ListingImage> imageEntities = new ArrayList<>();

		List<String> uploadedPublicIds = new ArrayList<>();

		try {
			for (int i = 0; i < files.size(); i++) {
				MultipartFile file = files.get(i);

				if (file == null || file.isEmpty()) {
					continue;
				}

				int displayOrder = currentImageCount + i + 1;

				String publicId = "listing_id" + listingId + "_" + System.currentTimeMillis() + "_" + displayOrder;

				ImageUploadResult uploadResult = imageUploadService.uploadImage(file, publicId);

				if (uploadResult != null && uploadResult.getPublicId() != null) {
					uploadedPublicIds.add(uploadResult.getPublicId());
				}

				ListingImage listingImage = modelMapper.imageUploadResultToListingImage(uploadResult, listingId,
						displayOrder);

				imageEntities.add(listingImage);
			}

			if (imageEntities.isEmpty()) {
				throw new IllegalArgumentException("No valid images were provided");
			}

			List<ListingImage> saveImages = listingImageRepo.saveAll(imageEntities);

			return saveImages.stream().map(modelMapper::listingImageToImageDto).collect(Collectors.toList());
		} catch (Exception ex) {

			rollbackUploadedImages(uploadedPublicIds);

			throw ex;
		}
	}

	@Override
	public void deleteListingImage(UUID userId, UUID listingId, UUID imageId) {
		checkWhetherSameManager(userId, listingId);

		ListingImage image = listingImageRepo.findById(imageId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Images not found: " + imageId));

		if (!image.getListingId().equals(listingId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image does not belong to this listing.");
		}

		// Delete from Cloudinary
		imageUploadService.deleteImage(image.getPublicId());

		// Delete from database
		listingImageRepo.delete(image);
	}
}
