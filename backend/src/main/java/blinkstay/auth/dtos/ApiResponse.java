package blinkstay.auth.dtos;

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
public class ApiResponse<T> {
	private Boolean success;
	private String message;
	private T data;

	// Static helper methods
	public static <T> ApiResponse<T> of(boolean success, String message, T data) {
		return ApiResponse.<T>builder().success(true).message(message).data(data).build();
	}

}
