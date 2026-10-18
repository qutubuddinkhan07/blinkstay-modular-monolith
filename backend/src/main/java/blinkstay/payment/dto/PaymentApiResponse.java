package blinkstay.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> PaymentApiResponse<T> success(String message, T data) {
        return PaymentApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> PaymentApiResponse<T> success(String message) {
        return PaymentApiResponse.<T>builder().success(true).message(message).data(null).build();
    }

    public static <T> PaymentApiResponse<T> failure(String message) {
        return PaymentApiResponse.<T>builder().success(false).message(message).data(null).build();
    }
}
