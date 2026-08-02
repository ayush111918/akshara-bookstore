package com.akshara.api.book.dto;

import java.time.Instant;
import java.util.List;

public record BookResponse(
        Long id,
        String title,
        String subtitle,
        String description,
        String coverImageUrl,
        String languageCode,
        List<AuthorResponse> authors,
        List<CategoryResponse> categories,
        List<BookEditionResponse> editions,
        Instant createdAt,
        Instant updatedAt
) {
}