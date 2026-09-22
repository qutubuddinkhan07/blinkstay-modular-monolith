package blinkstay.listing.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blinkstay.listing.entities.ListingGeometry;

public interface ListingGeometryRepository extends JpaRepository<ListingGeometry, UUID> {

}
