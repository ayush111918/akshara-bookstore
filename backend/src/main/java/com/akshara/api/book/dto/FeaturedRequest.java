package com.akshara.api.book.dto;

import jakarta.validation.constraints.NotNull;

public record FeaturedRequest(
        @NotNull(message = "Featured status is required")
        Boolean featured
) {
}
