package com.akshara.api.book.dto;

import java.time.Instant;

public record PublisherResponse(
        Long id,
        String name,
        String websiteUrl,
        Instant createdAt,
        Instant updatedAt
) {
}