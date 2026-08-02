package com.akshara.api.review.controller;

import com.akshara.api.review.dto.BookReviewsResponse;
import com.akshara.api.review.dto.ReviewResponse;
import com.akshara.api.review.service.ReviewService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@Validated
public class PublicReviewController {

    private final ReviewService reviewService;

    public PublicReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/books/{bookId}/reviews")
    public BookReviewsResponse getBookReviews(@PathVariable @Positive Long bookId) {
        return reviewService.getBookReviews(bookId);
    }

    @GetMapping("/reviews/recent")
    public List<ReviewResponse> getRecent(
            @RequestParam(defaultValue = "6") @Min(1) @Max(20) int limit
    ) {
        return reviewService.getRecent(limit);
    }
}
