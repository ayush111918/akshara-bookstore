package com.akshara.api.book.dto;

import java.time.Instant;

public record AuthorResponse(
        Long id,
        String name,
        String biography,
        Instant createdAt,
        Instant updatedAt
) {
}