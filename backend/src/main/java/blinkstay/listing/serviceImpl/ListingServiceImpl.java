package blinkstay.listing.serviceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.common.exception.ManagerNotOwnerException;
import blinkstay.listing.constants.ImagePublicIds;
import blinkstay.listing.dto.AddListingDto;
import blinkstay.listing.dto.GeocodingResult;
import blinkstay.listing.dto.ImageDto;
import blinkstay.listing.dto.ImageUploadResult;
import blinkstay.listing.dto.ListingDetailsResponseDto;
import blinkstay.listing.dto.PagedResponse;
import blinkstay.listing.dto.PublishedListingDto;
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
		return listingRepo.findById(listingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
				"No listing present with this id: " + listingId));
	}

	/**
	 * Runs the action after the DB commit. If there is no active transaction it
	 * runs right away instead of throwing "Transaction synchronization is not
	 * active".
	 */
	private void runAfterCommit(Runnable action) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					action.run();
				}
			});
		} else {
			action.run();
		}
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

				// Generate unique public_id
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

			// 5. Save images to DB
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
	@Cacheable(value = "listingById", key = "#listingId")
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

		// Batch fetch geometries (merge function: never crash on a duplicate row)
		Map<UUID, ListingGeometry> geometryMap = listingGeometryRepo.findByListingIdIn(listingIds).stream()
				.collect(Collectors.toMap(ListingGeometry::getListingId, Function.identity(),
						(existing, replacement) -> existing));

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

	// EVICT PUBLISHED LISTINGS FEED WHEN A NEW LISTING IS PUBLISHED
	@Override
	@Transactional
	@Caching(evict = { @CacheEvict(value = "listingById", key = "#listingId"),
			@CacheEvict(value = "publishedListings", allEntries = true) })
	public String listingPublishService(UUID userId, UUID listingId) {
		checkWhetherSameManager(userId, listingId);

		Listing listing = helperGetListingById(listingId); // 404 if missing

		// Proper statuses (not RuntimeException -> 500) so the UI can show the reason
		if (!roomService.checkDoesListingHaveRooms(listingId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Listing cannot be published because it has no rooms");
		}

		if (listing.getStatus() == ListingStatus.PUBLISHED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Listing already published");
		}

		listing.setStatus(ListingStatus.PUBLISHED);

		listingRepo.save(listing);

		return listing.getTitle() + " having id: " + listing.getId() + " PUBLISHED";
	}

	// EVICT BOTH SINGLE LISTING CACHE AND SEARCH FEED CACHE ON UPDATE
	@Override
	@Transactional
	@Caching(evict = { @CacheEvict(value = "listingById", key = "#listingId"),
			@CacheEvict(value = "publishedListings", allEntries = true) })
	public String updateListing(UUID managerId, UUID listingId, AddListingDto dto) {
		checkWhetherSameManager(managerId, listingId); // only the owner may edit
		Listing listing = helperGetListingById(listingId); // 404 instead of a 500

		log.info("Updating listing {} for user: {}", listingId, managerId);

		Optional<ListingGeometry> existingGeometry = listingGeometryRepo.findByListingId(listingId);

		boolean placeChanged = !Objects.equals(listing.getLocation(), dto.getLocation())
				|| !Objects.equals(listing.getCountry(), dto.getCountry());

		// Geocode FIRST: if Mapbox fails, nothing has been changed yet.
		// Only call Mapbox when the place changed or there are no coordinates yet.
		GeocodingResult geocoding = (placeChanged || existingGeometry.isEmpty())
				? mapboxGeocodingService.getCoordinates(dto.getLocation(), dto.getCountry())
				: null;

		listing.setTitle(dto.getTitle());
		listing.setLocation(dto.getLocation());
		listing.setDescription(dto.getDescription());
		listing.setCountry(dto.getCountry());
		listing.setAmenities(dto.getAmenities());
		listing.setCategory(dto.getCategory());

		listingRepo.save(listing);

		if (geocoding != null) {
			// Update the existing row instead of inserting a second one
			ListingGeometry geometry = existingGeometry
					.orElseGet(() -> ListingGeometry.builder().listingId(listingId).build());

			geometry.setAddress(geocoding.getAddress());
			geometry.setLongitude(geocoding.getLongitude());
			geometry.setLatitude(geocoding.getLatitude());

			listingGeometryRepo.save(geometry);
		}

		return listing.getId().toString();
	}

	// EVICT SINGLE LISTING CACHE WHEN IMAGES CHANGE
	@Override
	@CacheEvict(value = "listingById", key = "#listingId")
	public List<ImageDto> addListingImages(UUID userId, UUID listingId, List<MultipartFile> files) {
		checkWhetherSameManager(userId, listingId);

		// to check whether the listing exists or not
		helperGetListingById(listingId);

		// Continue after the highest existing order (safe after deletes, no duplicates)
		int nextOrder = listingImageRepo.findMaxDisplayOrder(listingId);

		List<ListingImage> imageEntities = new ArrayList<>();

		List<String> uploadedPublicIds = new ArrayList<>();

		try {
			for (MultipartFile file : files) {

				if (file == null || file.isEmpty()) {
					continue;
				}

				nextOrder++;

				String publicId = "listing_" + listingId + "_" + System.currentTimeMillis() + "_" + nextOrder;

				ImageUploadResult uploadResult = imageUploadService.uploadImage(file, publicId);

				if (uploadResult != null && uploadResult.getPublicId() != null) {
					uploadedPublicIds.add(uploadResult.getPublicId());
				}

				ListingImage listingImage = modelMapper.imageUploadResultToListingImage(uploadResult, listingId,
						nextOrder);

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

	// EVICT SINGLE LISTING CACHE WHEN IMAGE IS DELETED
	@Override
	@CacheEvict(value = "listingById", key = "#listingId")
	public void deleteListingImage(UUID userId, UUID listingId, UUID imageId) {
		checkWhetherSameManager(userId, listingId);

		ListingImage image = listingImageRepo.findById(imageId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Images not found: " + imageId));

		if (!image.getListingId().equals(listingId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image does not belong to this listing.");
		}

		if (listingImageRepo.countByListingId(listingId) <= 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A listing must keep at least one image.");
		}

		// Delete the DB row FIRST: if the Cloudinary call fails afterwards, the worst
		// case is an orphan file, never a listing with a broken image link
		listingImageRepo.delete(image);

		if (ImagePublicIds.isSeed(image)) {
			// Seed images are shared files used by many listings: remove only this row
			log.info("Image {} is a seed image: skipping Cloudinary delete", imageId);
		} else {
			try {
				imageUploadService.deleteImage(image.getPublicId());
			} catch (Exception e) {
				log.error("Failed to delete Cloudinary image {} (DB row already removed)", image.getPublicId(), e);
			}
		}
	}

	// CACHE PUBLISHED LISTINGS PAGINATED FEED
	@Override
	@Cacheable(value = "publishedListings", key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction + '-' + (#country != null ? #country : 'ALL')")
	public PagedResponse<PublishedListingDto> getPublishedListings(int page, int size, String sortBy, String direction,
			String country) {

		sortBy = validateSortFilter(sortBy);
		Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

		Page<Listing> listingPage;
		if (country != null && !country.isBlank()) {
			listingPage = listingRepo.findByStatusAndCountry(ListingStatus.PUBLISHED, country, pageable);
		} else {
			listingPage = listingRepo.findByStatus(ListingStatus.PUBLISHED, pageable);
		}

		List<Listing> listings = listingPage.getContent();

		if (listings.isEmpty()) {
			return PagedResponse.from(new PageImpl<>(List.of(), pageable, listingPage.getTotalElements()));
		}

		List<UUID> listingIds = listings.stream().map(Listing::getId).toList();
		List<ListingImage> images = listingImageRepo.findByListingIdInOrderByDisplayOrderAsc(listingIds);
		List<ListingGeometry> geometries = listingGeometryRepo.findByListingIdIn(listingIds);

		Map<UUID, String> coverImages = images.stream().collect(Collectors.toMap(ListingImage::getListingId,
				ListingImage::getImageUrl, (existing, replacement) -> existing));

		// merge function: never crash on a duplicate geometry row
		Map<UUID, ListingGeometry> geometryMap = geometries.stream().collect(Collectors
				.toMap(ListingGeometry::getListingId, geometry -> geometry, (existing, replacement) -> existing));

		List<PublishedListingDto> dtoList = listings.stream().map(listing -> {
			ListingGeometry geometry = geometryMap.get(listing.getId());

			return PublishedListingDto.builder().id(listing.getId()).title(listing.getTitle())
					.location(listing.getLocation()).country(listing.getCountry()).category(listing.getCategory())
					.amenities(listing.getAmenities()).coverImageUrl(coverImages.get(listing.getId()))
					.address(geometry != null ? geometry.getAddress() : null)
					.latitude(geometry != null ? geometry.getLatitude() : null)
					.longitude(geometry != null ? geometry.getLongitude() : null).build();
		}).toList();

		Page<PublishedListingDto> finalPage = new PageImpl<>(dtoList, pageable, listingPage.getTotalElements());

		return PagedResponse.from(finalPage);
	}

	private String validateSortFilter(String sortBy) {
		if (sortBy == null || sortBy.isBlank()) {
			return "createdAt";
		}

		return switch (sortBy.toLowerCase()) {
		case "title" -> "title";
		case "country" -> "country";
		case "location" -> "location";
		case "createdat" -> "createdAt";
		case "updatedat" -> "updatedAt";
		default -> "createdAt";
		};
	}

	@Override
	@Transactional
	@Caching(evict = { @CacheEvict(value = "listingById", key = "#listingId"),
			@CacheEvict(value = "publishedListings", allEntries = true) })
	public void deleteListing(UUID userId, boolean isAdmin, UUID listingId) {
		log.info("Deleting listing {} requested by {}", listingId, userId);

		Listing listing = helperGetListingById(listingId); // 404 if missing

		// 1. Owner only (admins may delete any listing)
		if (!isAdmin) {
			checkWhetherSameManager(userId, listingId);
		}

		// 2. When reservations exist, block deleting a listing that has active ones:
		// if (reservationService.hasActiveReservations(listingId)) {
		// throw new ResponseStatusException(HttpStatus.CONFLICT,
		// "This listing has active reservations");
		// }

		// 3. Remember the Cloudinary ids before the rows disappear
		List<ListingImage> images = listingImageRepo.findByListingIdOrderByDisplayOrderAsc(listingId);

		List<String> publicIds = images.stream().filter(img -> !ImagePublicIds.isSeed(img)) // never delete the shared
																							// seed files
				.map(ListingImage::getPublicId).filter(Objects::nonNull).toList();

		// 4. Delete children first, then the listing
		roomService.deleteRoomsByListingId(listingId);
		listingImageRepo.deleteAll(images);
		listingGeometryRepo.findByListingId(listingId).ifPresent(listingGeometryRepo::delete);
		listingRepo.delete(listing);

		// 5. Delete the Cloudinary files only AFTER the commit succeeds
		runAfterCommit(() -> deleteImagesFromCloudinary(publicIds));
	}

	private void deleteImagesFromCloudinary(List<String> publicIds) {
		for (String publicId : publicIds) {
			try {
				imageUploadService.deleteImage(publicId);
			} catch (Exception e) {
				log.error("Failed to delete Cloudinary image {} after listing delete", publicId, e);
			}
		}
	}

	@Override
	@Transactional
	@Caching(evict = { @CacheEvict(value = "listingById", key = "#listingId"),
			@CacheEvict(value = "publishedListings", allEntries = true) })
	public void deleteRoomFromListing(UUID userId, UUID listingId, UUID roomId) {
		checkWhetherSameManager(userId, listingId);
		Listing listing = helperGetListingById(listingId);

		roomService.deleteRoom(listingId, roomId);

		// A published listing must always have at least one room (publishing requires
		// it)
		if (listing.getStatus() == ListingStatus.PUBLISHED && !roomService.checkDoesListingHaveRooms(listingId)) {
			listing.setStatus(ListingStatus.DRAFT);
			listingRepo.save(listing);
			log.info("Listing {} moved back to DRAFT because its last room was deleted", listingId);
		}
	}
}