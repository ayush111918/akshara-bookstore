package com.akshara.api.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewReplyRequest(
        @NotBlank(message = "Reply text is required")
        @Size(min = 2, max = 1000, message = "Reply must contain between 2 and 1000 characters")
        String content
) {
}
