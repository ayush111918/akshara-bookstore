package com.akshara.api.wishlist.dto;

import com.akshara.api.book.dto.BookResponse;

import java.time.Instant;

public record WishlistItemResponse(
        Long wishlistItemId,
        BookResponse book,
        Instant addedAt
) {
}
