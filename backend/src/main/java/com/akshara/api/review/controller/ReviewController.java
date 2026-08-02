package com.akshara.api.review.controller;

import com.akshara.api.review.dto.ReviewRequest;
import com.akshara.api.review.dto.ReviewResponse;
import com.akshara.api.review.dto.UpdateReviewRequest;
import com.akshara.api.review.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.create(jwt.getSubject(), request));
    }

    @PutMapping("/{reviewId}")
    public ReviewResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        return reviewService.update(jwt.getSubject(), reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive Long reviewId
    ) {
        reviewService.delete(jwt.getSubject(), reviewId);
        return ResponseEntity.noContent().build();
    }
}
