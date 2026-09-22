package blinkstay.listing.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import blinkstay.listing.dtos.AddListingDto;
import blinkstay.listing.dtos.ImageUploadResult;
import blinkstay.listing.entities.Listing;
import blinkstay.listing.entities.ListingImage;
import blinkstay.listing.mapper.ModelMapper;
import blinkstay.listing.repository.ListingGeometryRepository;
import blinkstay.listing.repository.ListingImageRepository;
import blinkstay.listing.repository.ListingRepository;
import blinkstay.listing.service.ImageUploadService;
import blinkstay.listing.service.ListingService;
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

	@Qualifier("listingModelMapper")
	private final ModelMapper modelMapper;

	@Override
	@Transactional
	public String createListing(UUID managerId, AddListingDto addListingDto, List<MultipartFile> files) {
		log.info("Creating listing for user: {}", managerId);

		// Track successfully uploaded public IDs for potential cleanup
		List<String> uploadedPublicIds = new ArrayList<>();

		try {
			// 1. Map and save listing entity
			Listing listing = modelMapper.addListingDtoToListing(addListingDto, managerId);
			Listing savedListing = listingRepo.save(listing);

			List<ListingImage> imageEntities = new ArrayList<>();

			// 2. Process image uploads
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

}
