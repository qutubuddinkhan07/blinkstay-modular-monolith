package com.blinkstay.listing.repository;

import com.blinkstay.listing.entities.Listing;
import com.blinkstay.listing.enums.ListingStatus;
import com.blinkstay.listing.enums.SuspensionSource;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {
    @Query("SELECT l FROM Listing l WHERE l.managerId = :managerId")
    List<Listing> findAllByManagerId(@Param("managerId") UUID managerId);

    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

    List<Listing> findByManagerIdAndStatusNot(UUID managerId, ListingStatus status);

    List<Listing> findByManagerIdAndStatusAndSuspensionSource(UUID managerId, ListingStatus status,
                                                              SuspensionSource suspensionSource);

    long countByStatus(ListingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Listing l WHERE l.id = :id")
    Optional<Listing> findByIdForUpdate(@Param("id") UUID id);

    // bulk update: does not touch updatedAt, so the feed order never changes
    @Modifying
    @Query("UPDATE Listing l SET l.averageRating = :avg, l.reviewCount = :count WHERE l.id = :id")
    int updateRating(@Param("id") UUID id, @Param("avg") BigDecimal avg, @Param("count") int count);
}
