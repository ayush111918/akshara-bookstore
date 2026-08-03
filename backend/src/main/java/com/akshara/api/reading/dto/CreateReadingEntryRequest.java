package com.akshara.api.reading.dto;

import com.akshara.api.reading.entity.ReadingSourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReadingEntryRequest(
        @NotNull ReadingSourceType sourceType,
        @NotNull @Positive Long sourceId,
        @Positive Integer totalPages
) {
}
