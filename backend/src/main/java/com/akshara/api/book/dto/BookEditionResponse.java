package com.akshara.api.book.dto;

import com.akshara.api.book.entity.BookFormat;

import java.time.Instant;
import java.time.LocalDate;

public record BookEditionResponse(
        Long id,
        PublisherResponse publisher,
        BookFormat format,
        String editionName,
        String isbn10,
        String isbn13,
        LocalDate publicationDate,
        Integer pageCount,
        String sku,
        String externalEditionId,
        InventoryResponse inventory,
        Instant createdAt,
        Instant updatedAt
) {
}
