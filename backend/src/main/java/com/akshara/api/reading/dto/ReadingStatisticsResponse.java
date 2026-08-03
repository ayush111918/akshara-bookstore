package com.akshara.api.reading.dto;

public record ReadingStatisticsResponse(
        long trackedBooks,
        long currentlyReading,
        long completedBooks,
        long completedThisYear,
        long pagesReadThisYear,
        int activeDaysThisYear,
        int currentStreak,
        int longestStreak
) {
}
