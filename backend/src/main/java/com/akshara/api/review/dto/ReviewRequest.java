package com.akshara.api.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull(message = "Book ID is required")
        @Positive(message = "Book ID must be positive")
        Long bookId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must not exceed 5")
        Integer rating,

        @Size(max = 120, message = "Review headline must not exceed 120 characters")
        String headline,

        @NotBlank(message = "Review text is required")
        @Size(min = 10, max = 2000, message = "Review text must contain between 10 and 2000 characters")
        String content
) {
}
