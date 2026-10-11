package blinkstay.listing.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import blinkstay.listing.entities.Listing;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.listing.enums.SuspensionSource;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
	@Query("SELECT l FROM Listing l WHERE l.managerId = :managerId")
	List<Listing> findAllByManagerId(@Param("managerId") UUID managerId);

	Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

	@Query("""
			SELECT l FROM Listing l WHERE l.status = :status
			AND (:location IS NULL OR LOWER(l.location) LIKE LOWER(CONCAT('%', :location, '%')))
			AND (:name IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :name, '%')))
			""")
	Page<Listing> findByStatusAndFilters(@Param("status") ListingStatus listingStatus,
			@Param("location") String location, @Param("name") String name, Pageable pageable);

	List<Listing> findByManagerIdAndStatusNot(UUID managerId, ListingStatus status);

	List<Listing> findByManagerIdAndStatusAndSuspensionSource(UUID managerId, ListingStatus status,
			SuspensionSource suspensionSource);
}
