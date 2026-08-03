package com.akshara.api.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(

        @NotNull(message = "Book edition ID is required")
        @Positive(message = "Book edition ID must be positive")
        Long bookEditionId,

        @NotNull(message = "Quantity is required")
        @Min(
                value = 1,
                message = "Quantity must be at least 1"
        )
        Integer quantity
) {
}