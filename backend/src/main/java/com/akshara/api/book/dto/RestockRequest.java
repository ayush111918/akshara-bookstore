package com.akshara.api.book.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RestockRequest(
        @NotNull(message = "Restock quantity is required")
        @Positive(message = "Restock quantity must be greater than zero")
        @Max(value = 1_000_000, message = "Restock quantity must not exceed 1000000")
        Integer quantity
) {
}
