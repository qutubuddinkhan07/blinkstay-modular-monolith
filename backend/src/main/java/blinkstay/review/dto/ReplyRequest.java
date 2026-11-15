package blinkstay.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplyRequest(
        @NotBlank(message = "Reply cannot be empty") @Size(max = 1000, message = "Reply can be at most 1000 characters") String reply) {
}