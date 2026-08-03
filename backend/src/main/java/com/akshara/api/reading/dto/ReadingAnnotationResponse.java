package com.akshara.api.reading.dto;

import com.akshara.api.reading.entity.ReadingAnnotationType;

import java.time.Instant;

public record ReadingAnnotationResponse(
        Long id,
        Long readingEntryId,
        ReadingAnnotationType type,
        String content,
        Integer pageNumber,
        Instant createdAt,
        Instant updatedAt
) {
}
