package com.akshara.api.cart.dto;

import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.BookFormat;

import java.math.BigDecimal;

public record CartItemResponse(
        Long itemId,

        Long bookId,
        String title,
        String coverImageUrl,

        Long bookEditionId,
        BookFormat format,
        String editionName,
        String isbn10,
        String isbn13,
        String publisherName,

        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,

        Integer availableStock,
        AvailabilityStatus availabilityStatus,
        boolean availableForPurchase
) {
}