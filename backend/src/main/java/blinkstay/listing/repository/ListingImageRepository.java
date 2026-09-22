package blinkstay.listing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blinkstay.listing.entities.ListingImage;

public interface ListingImageRepository extends JpaRepository<ListingImage, UUID> {
	List<ListingImage> findByListingIdOrderByDisplayOrderAsc(UUID listingId);

	// Finding data in batch
	List<ListingImage> findByListingIdInOrderByDisplayOrderAsc(List<UUID> listingIds);

	Optional<ListingImage> findFirstByListingIdOrderByDisplayOrderAsc(UUID listingId);
}
