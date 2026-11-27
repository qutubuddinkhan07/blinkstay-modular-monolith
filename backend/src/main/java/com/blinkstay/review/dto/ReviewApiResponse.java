package com.blinkstay.review.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ReviewApiResponse<T> success(String message, T data) {
        return ReviewApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> ReviewApiResponse<T> success(String message) {
        return ReviewApiResponse.<T>builder().success(true).message(message).data(null).build();
    }
}
