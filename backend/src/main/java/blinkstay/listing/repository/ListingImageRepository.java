package blinkstay.listing.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blinkstay.listing.entities.ListingImage;

public interface ListingImageRepository extends JpaRepository<ListingImage, UUID> {

}
