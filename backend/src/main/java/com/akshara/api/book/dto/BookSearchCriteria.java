package com.akshara.api.book.dto;

import com.akshara.api.book.entity.BookFormat;

import java.math.BigDecimal;

public record BookSearchCriteria(
        String query,
        Long authorId,
        Long categoryId,
        String languageCode,
        BookFormat format,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean inStock
) {
}