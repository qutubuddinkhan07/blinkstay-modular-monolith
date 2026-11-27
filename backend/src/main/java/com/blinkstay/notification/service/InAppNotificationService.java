package com.blinkstay.notification.service;

import java.util.UUID;

import com.blinkstay.common.dto.PageResult;
import com.blinkstay.notification.dto.NotificationDto;
import com.blinkstay.notification.enums.NotificationType;

public interface InAppNotificationService {
	void create(UUID userId, NotificationType type, String title, String message, UUID referenceId);

	PageResult<NotificationDto> list(UUID userId, boolean unreadOnly, int page, int size);

	long unreadCount(UUID userId);

	void markRead(UUID userId, UUID notificationId);

	int markAllRead(UUID userId);

	void delete(UUID userId, UUID notificationId);

	int clear(UUID userId, boolean onlyRead);
}
