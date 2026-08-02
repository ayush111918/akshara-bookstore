package com.akshara.api.book.dto;

import com.akshara.api.book.entity.AvailabilityStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryResponse(
        Long id,
        BigDecimal price,
        Integer stockQuantity,
        AvailabilityStatus availabilityStatus,
        boolean active,
        Instant updatedAt
) {
}