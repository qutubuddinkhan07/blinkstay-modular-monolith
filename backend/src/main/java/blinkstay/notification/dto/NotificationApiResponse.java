package blinkstay.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationApiResponse<T> {
	private boolean success;
	private String message;
	private T data;

	public static <T> NotificationApiResponse<T> success(String message, T data) {
		return NotificationApiResponse.<T>builder().success(true).message(message).data(data).build();
	}

	public static <T> NotificationApiResponse<T> success(String message) {
		return NotificationApiResponse.<T>builder().success(false).message(message).data(null).build();
	}

	public static <T> NotificationApiResponse<T> failure(String message) {
		return NotificationApiResponse.<T>builder().success(false).message(message).data(null).build();
	}
}
