package com.akshara.api.order.dto;

import com.akshara.api.book.entity.BookFormat;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long orderItemId,
        Long bookEditionId,
        String bookTitle,
        String coverImageUrl,
        BookFormat bookFormat,
        String editionName,
        String isbn10,
        String isbn13,
        String publisherName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}