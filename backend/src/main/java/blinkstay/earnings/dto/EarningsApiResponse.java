package blinkstay.earnings.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningsApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> EarningsApiResponse<T> success(String message, T data) {
        return EarningsApiResponse.<T>builder().success(true).message(message).data(data).build();
    }
}
