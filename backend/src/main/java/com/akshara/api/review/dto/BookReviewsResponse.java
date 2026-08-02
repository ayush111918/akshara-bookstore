package com.akshara.api.review.dto;

import java.util.List;

public record BookReviewsResponse(
        Long bookId,
        long reviewCount,
        double averageRating,
        List<ReviewResponse> reviews
) {
}
