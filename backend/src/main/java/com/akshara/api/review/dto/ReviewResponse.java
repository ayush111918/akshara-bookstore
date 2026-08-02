package com.akshara.api.review.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String coverImageUrl,
        Long userId,
        String readerName,
        Integer rating,
        String headline,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
}
