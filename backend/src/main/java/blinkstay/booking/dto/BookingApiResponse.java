package blinkstay.booking.dto;

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
public class BookingApiResponse<T> {
	private boolean success;
	private String message;
	private T data;

	public static <T> BookingApiResponse<T> success(String message, T data) {
		return BookingApiResponse.<T>builder().success(true).message(message).data(data).build();
	}

	public static <T> BookingApiResponse<T> success(String message) {
		return BookingApiResponse.<T>builder().success(true).message(message).data(null).build();
	}

	public static <T> BookingApiResponse<T> failure(String message) {
		return BookingApiResponse.<T>builder().success(false).message(message).data(null).build();
	}
}