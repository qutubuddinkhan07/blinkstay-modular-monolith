package blinkstay.listing.serviceImpl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import blinkstay.listing.repository.ListingRepository;
import blinkstay.listing.service.CloudinaryOrphanCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryOrphanCleanupServiceImpl implements CloudinaryOrphanCleanupService {

	private final Cloudinary cloudinary;
	private final ListingRepository listingRepository;

	// Pattern to match standard UUID v4 format (8-4-4-4-12 hex characters)
	private static final Pattern UUID_PATTERN = Pattern
			.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

	/**
	 * Finds and deletes orphan images from Cloudinary folders whose listing IDs no
	 * longer exist in the database.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void cleanupOrphanListingImages() {
		log.info("Starting Cloudinary orphan image cleanup...");

		try {
			// 1. Fetch all resources inside the 'blinkstay_listings' folder
			Map<String, Object> response = cloudinary.api().resources(
					ObjectUtils.asMap("type", "upload", "prefix", "blinkstay_listings/", "max_results", 500));

			List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("resources");

			if (resources == null || resources.isEmpty()) {
				log.info("No images found in Cloudinary under 'blinkstay_listings/'.");
				return;
			}

			for (Map<String, Object> resource : resources) {
				String publicId = (String) resource.get("public_id");

				if (publicId != null) {
					Matcher matcher = UUID_PATTERN.matcher(publicId);

					// Safely extract the UUID from the publicId regardless of folder prefix or
					// prefix string
					if (matcher.find()) {
						try {
							String listingIdStr = matcher.group();
							UUID listingId = UUID.fromString(listingIdStr);

							// 2. Check if the listing exists in the database
							boolean exists = listingRepository.existsById(listingId);

							if (!exists) {
								log.warn("Orphan image detected! Listing ID {} does not exist. Deleting image {}",
										listingId, publicId);

								// 3. Delete orphan image from Cloudinary
								cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
							}
						} catch (IllegalArgumentException e) {
							log.warn("Could not parse UUID from publicId: {}", publicId);
						}
					} else {
						log.debug("No UUID found in publicId: {}", publicId);
					}
				}
			}

			log.info("Cloudinary orphan cleanup completed successfully.");

		} catch (Exception e) {
			log.error("Failed to execute Cloudinary orphan cleanup", e);
		}
	}
}