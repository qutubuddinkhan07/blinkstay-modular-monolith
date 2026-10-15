package blinkstay.notification.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.common.dto.PageResult;
import blinkstay.notification.dto.NotificationApiResponse;
import blinkstay.notification.dto.NotificationDto;
import blinkstay.notification.service.InAppNotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {
	private final InAppNotificationService service;

	@GetMapping
	public ResponseEntity<NotificationApiResponse<PageResult<NotificationDto>>> list(
			@AuthenticationPrincipal UserDetails userDetails, @RequestParam(defaultValue = "false") boolean unreadOnly,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ok("Notifications", service.list(UUID.fromString(userDetails.getUsername()), unreadOnly, page, size));
	}

	@GetMapping("/unread-count")
	public ResponseEntity<NotificationApiResponse<Long>> unreadCount(@AuthenticationPrincipal UserDetails userDetails) {
		return ok("Unread notification", service.unreadCount(UUID.fromString(userDetails.getUsername())));
	}

	@PatchMapping("/{id}/read")
	public ResponseEntity<NotificationApiResponse<String>> markRead(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable("id") UUID id) {
		return ok("Marked as read", null);
	}

	@PatchMapping("/read-all")
	public ResponseEntity<NotificationApiResponse<Integer>> markAllRead(
			@AuthenticationPrincipal UserDetails userDetails) {
		return ok("All marked as read", service.markAllRead(UUID.fromString(userDetails.getUsername())));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<NotificationApiResponse<String>> delete(@AuthenticationPrincipal UserDetails ud,
			@PathVariable("id") UUID id) {
		service.delete(UUID.fromString(ud.getUsername()), id);
		return ok("Notification deleted", null);
	}

	// scope defaults to the safe option: only read notifications are removed
	@DeleteMapping
	public ResponseEntity<NotificationApiResponse<Integer>> clear(@AuthenticationPrincipal UserDetails ud,
			@RequestParam(defaultValue = "read") String scope) {
		boolean onlyRead = switch (scope.toLowerCase()) {
		case "read" -> true;
		case "all" -> false;
		default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope must be 'read' or 'all'");
		};
		return ok(onlyRead ? "Read notifications cleared" : "All notifications cleared",
				service.clear(UUID.fromString(ud.getUsername()), onlyRead));
	}

	private static <T> ResponseEntity<NotificationApiResponse<T>> ok(String message, T data) {
		return ResponseEntity.ok(NotificationApiResponse.success(message, data));
	}
}
