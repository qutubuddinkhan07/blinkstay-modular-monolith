package blinkstay.listing.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import blinkstay.listing.entities.Listing;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.listing.enums.SuspensionSource;

public interface ListingRepository extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {
	@Query("SELECT l FROM Listing l WHERE l.managerId = :managerId")
	List<Listing> findAllByManagerId(@Param("managerId") UUID managerId);

	Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

	List<Listing> findByManagerIdAndStatusNot(UUID managerId, ListingStatus status);

	List<Listing> findByManagerIdAndStatusAndSuspensionSource(UUID managerId, ListingStatus status,
			SuspensionSource suspensionSource);
}
