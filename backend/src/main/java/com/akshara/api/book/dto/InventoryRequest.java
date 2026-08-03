package com.akshara.api.book.dto;

import com.akshara.api.book.entity.AvailabilityStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record InventoryRequest(

        @NotNull(message = "Price is required")
        @DecimalMin(
                value = "0.01",
                message = "Price must be greater than zero"
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "Price must contain at most 8 integer digits and 2 decimal places"
        )
        BigDecimal price,

        @NotNull(message = "Stock quantity is required")
        @PositiveOrZero(
                message = "Stock quantity must not be negative"
        )
        Integer stockQuantity,

        @NotNull(message = "Availability status is required")
        AvailabilityStatus availabilityStatus,

        @NotNull(message = "Active status is required")
        Boolean active
) {
}
