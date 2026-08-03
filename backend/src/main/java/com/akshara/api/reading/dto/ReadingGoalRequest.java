package com.akshara.api.reading.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReadingGoalRequest(
        @NotNull @Min(1) @Max(1000) Integer targetBooks
) {
}
