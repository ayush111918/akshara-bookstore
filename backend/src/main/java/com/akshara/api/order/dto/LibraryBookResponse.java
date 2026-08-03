package com.akshara.api.order.dto;

import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.order.entity.OrderStatus;

import java.time.Instant;

public record LibraryBookResponse(
        Long orderItemId,
        Long orderId,
        Long bookId,
        Long bookEditionId,
        String title,
        String coverImageUrl,
        BookFormat format,
        String editionName,
        String isbn,
        String publisherName,
        int quantity,
        OrderStatus orderStatus,
        Instant purchasedAt
) {
}
