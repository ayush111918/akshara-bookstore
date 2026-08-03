package com.akshara.api.library.dto;

import com.akshara.api.book.entity.BookFormat;

import java.time.Instant;

public record PersonalBookResponse(
        Long id,
        String title,
        String author,
        BookFormat format,
        String originalFilename,
        long fileSize,
        Instant uploadedAt
) {
}
