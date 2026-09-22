package blinkstay.listing.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blinkstay.listing.entities.Listing;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

}
