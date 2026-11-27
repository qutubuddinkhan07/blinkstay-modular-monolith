package com.blinkstay.review.dto;

import jakarta.validation.constraints.*;

public record ReviewRequest(
        @NotNull(message = "Rating is required") @Min(value = 1, message = "Rating must be 1 to 5") @Max(value = 5, message = "Rating must be 1 to 5") Integer rating,
        @NotBlank(message = "Please write a few words about your stay")
        @Size(min = 10, max = 1000, message = "Comment must be between 10 and 1000 characters")
        String comment) {
}