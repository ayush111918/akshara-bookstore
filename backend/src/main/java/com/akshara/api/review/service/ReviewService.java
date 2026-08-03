package com.akshara.api.review.service;

import com.akshara.api.book.entity.Book;
import com.akshara.api.book.repository.BookRepository;
import com.akshara.api.common.exception.DuplicateResourceException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.review.dto.BookReviewsResponse;
import com.akshara.api.review.dto.ReviewRequest;
import com.akshara.api.review.dto.ReviewResponse;
import com.akshara.api.review.dto.UpdateReviewRequest;
import com.akshara.api.review.dto.ReviewReplyRequest;
import com.akshara.api.review.dto.ReviewReplyResponse;
import com.akshara.api.review.entity.Review;
import com.akshara.api.review.entity.ReviewReply;
import com.akshara.api.review.repository.ReviewRepository;
import com.akshara.api.review.repository.ReviewReplyRepository;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserService userService;
    private final ReviewReplyRepository reviewReplyRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookRepository bookRepository,
            UserService userService,
            ReviewReplyRepository reviewReplyRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userService = userService;
        this.reviewReplyRepository = reviewReplyRepository;
    }

    @Transactional(readOnly = true)
    public BookReviewsResponse getBookReviews(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found with id: " + bookId);
        }

        List<ReviewResponse> reviews = reviewRepository
                .findAllByBook_IdOrderByCreatedAtDesc(bookId)
                .stream()
                .map(this::toResponse)
                .toList();

        double average = reviews.stream()
                .mapToInt(ReviewResponse::rating)
                .average()
                .orElse(0.0);

        return new BookReviewsResponse(bookId, reviews.size(), average, reviews);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getRecent(int limit) {
        return reviewRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMine(String subject) {
        AppUser user = userService.getCurrentUserEntity(subject);
        return reviewRepository.findAllByUser_IdOrderByUpdatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewReplyResponse> getReplies(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review with ID " + reviewId + " was not found");
        }
        return reviewReplyRepository.findAllByReview_IdOrderByCreatedAtAsc(reviewId)
                .stream().map(this::toReplyResponse).toList();
    }

    public ReviewReplyResponse createReply(String subject, Long reviewId, ReviewReplyRequest request) {
        AppUser user = userService.getCurrentUserEntity(subject);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " was not found"));
        ReviewReply reply = new ReviewReply(review, user, request.content().trim());
        return toReplyResponse(reviewReplyRepository.save(reply));
    }

    public ReviewReplyResponse updateReply(String subject, Long replyId, ReviewReplyRequest request) {
        AppUser user = userService.getCurrentUserEntity(subject);
        ReviewReply reply = findOwnedReply(replyId, user.getId());
        reply.update(request.content().trim());
        return toReplyResponse(reviewReplyRepository.save(reply));
    }

    public void deleteReply(String subject, Long replyId) {
        AppUser user = userService.getCurrentUserEntity(subject);
        reviewReplyRepository.delete(findOwnedReply(replyId, user.getId()));
    }

    public ReviewResponse create(String subject, ReviewRequest request) {
        AppUser user = userService.getCurrentUserEntity(subject);

        if (reviewRepository.findByUser_IdAndBook_Id(user.getId(), request.bookId()).isPresent()) {
            throw new DuplicateResourceException("You have already reviewed this book");
        }

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book not found with id: " + request.bookId()
                ));

        Review review = new Review(
                user,
                book,
                request.rating(),
                normalizeHeadline(request.headline()),
                request.content().trim()
        );

        return toResponse(reviewRepository.save(review));
    }

    public ReviewResponse update(String subject, Long reviewId, UpdateReviewRequest request) {
        AppUser user = userService.getCurrentUserEntity(subject);
        Review review = findOwned(reviewId, user.getId());

        review.update(
                request.rating(),
                normalizeHeadline(request.headline()),
                request.content().trim()
        );

        return toResponse(reviewRepository.save(review));
    }

    public void delete(String subject, Long reviewId) {
        AppUser user = userService.getCurrentUserEntity(subject);
        reviewRepository.delete(findOwned(reviewId, user.getId()));
    }

    private Review findOwned(Long reviewId, Long userId) {
        return reviewRepository.findById(reviewId)
                .filter(review -> review.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review with ID " + reviewId + " was not found"
                ));
    }

    private ReviewReply findOwnedReply(Long replyId, Long userId) {
        return reviewReplyRepository.findById(replyId)
                .filter(reply -> reply.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Discussion reply with ID " + replyId + " was not found"));
    }

    private String normalizeHeadline(String headline) {
        if (headline == null || headline.isBlank()) {
            return null;
        }
        return headline.trim();
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBook().getId(),
                review.getBook().getTitle(),
                review.getBook().getCoverImageUrl(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getHeadline(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private ReviewReplyResponse toReplyResponse(ReviewReply reply) {
        return new ReviewReplyResponse(
                reply.getId(), reply.getReview().getId(), reply.getUser().getId(),
                reply.getUser().getFullName(), reply.getContent(), reply.getCreatedAt(), reply.getUpdatedAt()
        );
    }
}
