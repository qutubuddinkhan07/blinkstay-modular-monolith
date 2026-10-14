package blinkstay.notification.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import blinkstay.notification.entities.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

	Page<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

	long countByUserIdAndReadAtIsNull(UUID userId);

	Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

	@Modifying(clearAutomatically = true)
	@Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
	int markAllRead(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

	@Modifying
	@Query("delete from Notification n where n.createdAt < :cutoff")
	int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
