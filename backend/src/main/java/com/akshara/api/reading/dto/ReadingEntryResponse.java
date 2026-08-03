package com.akshara.api.reading.dto;

import com.akshara.api.reading.entity.ReadingSourceType;
import com.akshara.api.reading.entity.ReadingStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ReadingEntryResponse(
        Long id,
        ReadingSourceType sourceType,
        Long sourceId,
        Long bookId,
        String title,
        String author,
        String coverImageUrl,
        ReadingStatus status,
        Integer currentPage,
        Integer totalPages,
        Integer progressPercentage,
        LocalDate startedOn,
        LocalDate completedOn,
        LocalDate lastActivityOn,
        long annotationCount,
        boolean reviewAvailable,
        Instant updatedAt
) {
}
