package blinkstay.listing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blinkstay.listing.entities.ListingGeometry;

public interface ListingGeometryRepository extends JpaRepository<ListingGeometry, UUID> {
	Optional<ListingGeometry> findByListingId(UUID listingId);

	// Finding data in batch
	List<ListingGeometry> findByListingIdIn(List<UUID> listingIds);
}
