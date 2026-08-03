package com.akshara.api.review.service;

import com.akshara.api.book.repository.BookRepository;
import com.akshara.api.common.exception.ResourceNotFoundException;
import com.akshara.api.review.dto.ReviewReplyRequest;
import com.akshara.api.review.entity.Review;
import com.akshara.api.review.entity.ReviewReply;
import com.akshara.api.review.repository.ReviewReplyRepository;
import com.akshara.api.review.repository.ReviewRepository;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewDiscussionServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock BookRepository bookRepository;
    @Mock UserService userService;
    @Mock ReviewReplyRepository reviewReplyRepository;
    @Mock AppUser user;
    @Mock AppUser anotherUser;
    @Mock Review review;
    @Mock ReviewReply reply;

    private ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(reviewRepository, bookRepository, userService, reviewReplyRepository);
    }

    @Test
    void createReplyShouldAttachAuthenticatedReaderToReview() {
        when(userService.getCurrentUserEntity("1")).thenReturn(user);
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));
        when(reviewReplyRepository.save(any(ReviewReply.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(user.getId()).thenReturn(1L);
        when(user.getFullName()).thenReturn("A Reader");
        when(review.getId()).thenReturn(7L);

        var response = service.createReply("1", 7L, new ReviewReplyRequest(" A thoughtful reply "));

        assertThat(response.reviewId()).isEqualTo(7L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("A thoughtful reply");
        ArgumentCaptor<ReviewReply> captor = ArgumentCaptor.forClass(ReviewReply.class);
        verify(reviewReplyRepository).save(captor.capture());
        assertThat(captor.getValue().getReview()).isSameAs(review);
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void updateReplyShouldHideAnotherReadersReply() {
        when(userService.getCurrentUserEntity("1")).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(reviewReplyRepository.findById(9L)).thenReturn(Optional.of(reply));
        when(reply.getUser()).thenReturn(anotherUser);
        when(anotherUser.getId()).thenReturn(2L);

        assertThatThrownBy(() -> service.updateReply("1", 9L, new ReviewReplyRequest("Changed")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Discussion reply with ID 9 was not found");
    }
}
