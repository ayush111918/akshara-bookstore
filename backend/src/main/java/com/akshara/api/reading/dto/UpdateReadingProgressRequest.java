package com.akshara.api.reading.dto;

import com.akshara.api.reading.entity.ReadingStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateReadingProgressRequest(
        @NotNull ReadingStatus status,
        @NotNull @Min(0) Integer currentPage,
        @Positive Integer totalPages
) {
}
