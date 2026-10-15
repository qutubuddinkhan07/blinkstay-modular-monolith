package blinkstay.notification.serviceImpl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.common.dto.PageResult;
import blinkstay.notification.dto.NotificationDto;
import blinkstay.notification.entities.Notification;
import blinkstay.notification.enums.NotificationType;
import blinkstay.notification.repository.NotificationRepository;
import blinkstay.notification.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationServiceImpl implements InAppNotificationService {
	private static final int RETENTION_DAYS = 90;

	private final NotificationRepository repo;

	// REQUIRES_NEW: this is called after the publisher's transaction has already
	// committed,
	// so the write needs a transaction of its own or it would never be saved
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void create(UUID userId, NotificationType type, String title, String message, UUID referenceId) {
		repo.save(Notification.builder().userId(userId).type(type).title(cut(title, 150)).message(cut(message, 1000))
				.referenceId(referenceId).build());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<NotificationDto> list(UUID userId, boolean unreadOnly, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
		Page<Notification> result = unreadOnly ? repo.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
				: repo.findByUserIdOrderByCreatedAtDesc(userId, pageable);

		return new PageResult<>(result.getContent().stream().map(this::toDto).toList(), result.getNumber(),
				result.getSize(), result.getTotalElements(), result.getTotalPages());
	}

	@Override
	@Transactional(readOnly = true)
	public long unreadCount(UUID userId) {
		return repo.countByUserIdAndReadAtIsNull(userId);
	}

	@Override
	@Transactional
	public void markRead(UUID userId, UUID notificationId) {
		// looked up by owner as well, so nobody can touch another user's row
		Notification n = repo.findByIdAndUserId(notificationId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

		if (n.getReadAt() == null) {
			n.setReadAt(LocalDateTime.now());
			repo.save(n);
		}
	}

	@Override
	@Transactional
	public int markAllRead(UUID userId) {
		return repo.markAllRead(userId, LocalDateTime.now());
	}

	@Scheduled(cron = "0 30 3 * * *") // every day at 03:30
	@Transactional
	public void purgeOld() {
		int removed = repo.deleteOlderThan(LocalDateTime.now().minusDays(RETENTION_DAYS));
		log.info("Notification cleanup removed {} rows older than {} days", removed, RETENTION_DAYS);
	}

	@Override
	@Transactional
	public void delete(UUID userId, UUID notificationId) {
		if (repo.deleteOwned(notificationId, userId) == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
		}
	}

	@Override
	@Transactional
	public int clear(UUID userId, boolean onlyRead) {
		return onlyRead ? repo.deleteRead(userId) : repo.deleteAllFor(userId);
	}

	private NotificationDto toDto(Notification n) {
		return NotificationDto.builder().id(n.getId()).type(n.getType()).title(n.getTitle()).message(n.getMessage())
				.referenceId(n.getReferenceId()).read(n.getReadAt() != null).readAt(n.getReadAt())
				.createdAt(n.getCreatedAt()).build();
	}

	private static String cut(String s, int max) {
		return s == null ? "" : (s.length() <= max ? s : s.substring(0, max - 1) + "\u2026");
	}
}
