package blinkstay.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import blinkstay.auth.entities.HotelManagerRequest;
import blinkstay.auth.enums.RequestStatus;

@Repository
public interface HotelManagerRequestRepository extends JpaRepository<HotelManagerRequest, UUID> {
	Optional<HotelManagerRequest> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<HotelManagerRequest> findByUserIdAndStatus(UUID userId, RequestStatus status);

	Page<HotelManagerRequest> findByStatus(RequestStatus status, Pageable pageable);

	long countByStatus(RequestStatus status);
}
