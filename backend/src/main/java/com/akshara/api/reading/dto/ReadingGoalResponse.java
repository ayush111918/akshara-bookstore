package com.akshara.api.reading.dto;

public record ReadingGoalResponse(
        Integer year,
        Integer targetBooks,
        long completedBooks,
        Integer progressPercentage
) {
}
