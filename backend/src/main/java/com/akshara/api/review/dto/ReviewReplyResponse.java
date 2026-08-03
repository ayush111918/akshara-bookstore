package com.akshara.api.review.dto;

import java.time.Instant;

public record ReviewReplyResponse(
        Long id,
        Long reviewId,
        Long userId,
        String readerName,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
}
