package com.akshara.api.review.repository;

import com.akshara.api.review.entity.ReviewReply;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
    @EntityGraph(attributePaths = "user")
    List<ReviewReply> findAllByReview_IdOrderByCreatedAtAsc(Long reviewId);
}
