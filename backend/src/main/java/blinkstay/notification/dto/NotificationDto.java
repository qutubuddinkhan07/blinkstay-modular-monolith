package blinkstay.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import blinkstay.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
	private UUID id;
	private NotificationType type;
	private String title;
	private String message;
	private UUID referenceId;
	private boolean read;
	private LocalDateTime readAt;
	private LocalDateTime createdAt;
}
