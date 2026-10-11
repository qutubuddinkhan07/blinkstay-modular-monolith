package blinkstay.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import blinkstay.auth.entities.HotelManagerProfile;

@Repository
public interface HotelManagerProfileRepository extends JpaRepository<HotelManagerProfile, UUID> {
	Optional<HotelManagerProfile> findByUserId(UUID userId);
}
