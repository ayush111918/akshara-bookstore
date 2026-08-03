package com.akshara.api.review.service;

import com.akshara.api.book.entity.Book;
import com.akshara.api.book.repository.BookRepository;
import com.akshara.api.order.entity.OrderStatus;
import com.akshara.api.order.repository.OrderItemRepository;
import com.akshara.api.reading.entity.ReadingStatus;
import com.akshara.api.reading.repository.ReadingEntryRepository;
import com.akshara.api.review.entity.Review;
import com.akshara.api.review.repository.ReviewReplyRepository;
import com.akshara.api.review.repository.ReviewRepository;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewTrustServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock BookRepository bookRepository;
    @Mock UserService userService;
    @Mock ReviewReplyRepository replyRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ReadingEntryRepository readingEntryRepository;
    @Mock Review review;
    @Mock AppUser user;
    @Mock Book book;

    private ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(
                reviewRepository, bookRepository, userService, replyRepository,
                orderItemRepository, readingEntryRepository
        );
    }

    @Test
    void exposesVerifiedPurchaseAndCompletedJourneyIndependently() {
        when(reviewRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(review.getUser()).thenReturn(user);
        when(review.getBook()).thenReturn(book);
        when(user.getId()).thenReturn(11L);
        when(user.getFullName()).thenReturn("A Reader");
        when(book.getId()).thenReturn(22L);
        when(book.getTitle()).thenReturn("A Book");
        when(orderItemRepository.existsByOrder_User_IdAndOrder_StatusAndBookEdition_Book_Id(
                11L, OrderStatus.DELIVERED, 22L
        )).thenReturn(true);
        when(readingEntryRepository.existsByUser_IdAndBookIdAndStatus(
                11L, 22L, ReadingStatus.COMPLETED
        )).thenReturn(false);

        var response = service.getRecent(10).get(0);

        assertThat(response.verifiedPurchase()).isTrue();
        assertThat(response.completedOnAkshara()).isFalse();
    }
}
