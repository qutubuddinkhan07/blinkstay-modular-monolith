package blinkstay.listing.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import blinkstay.listing.entities.Favorite;
import blinkstay.listing.entities.Listing;
import blinkstay.listing.enums.ListingStatus;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
	boolean existsByUserIdAndListingId(UUID userId, UUID listingId);

	long countByUserId(UUID userId);

	@Query("select f.listingId from Favorite f where f.userId = :userId order by f.createdAt desc")
	List<UUID> findListingIdsByUserId(@Param("userId") UUID userId);

	@Query(value = "select l from Favorite f join Listing l on l.id = f.listingId "
			+ "where f.userId = :userId and l.status = :status order by f.createdAt desc", countQuery = "select count(l) from Favorite f join Listing l on l.id = f.listingId "
					+ "where f.userId = :userId and l.status = :status")
	Page<Listing> findPublishedFavorites(@Param("userId") UUID userId, @Param("status") ListingStatus status,
			Pageable pageable);

	@Modifying
	@Query("delete from Favorite f where f.userId = :userId and f.listingId = :listingId")
	int deleteOne(@Param("userId") UUID userId, @Param("listingId") UUID listingId);

	@Modifying
	@Query("delete from Favorite f where f.listingId = :listingId")
	int deleteAllForListing(@Param("listingId") UUID listingId);

	@Modifying
	@Query("delete from Favorite f where f.userId = :userId")
	int deleteAllForUser(@Param("userId") UUID userId);
}