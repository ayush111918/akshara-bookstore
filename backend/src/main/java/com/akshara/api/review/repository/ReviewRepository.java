package com.akshara.api.review.repository;

import com.akshara.api.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByBook_IdOrderByCreatedAtDesc(Long bookId);

    Optional<Review> findByUser_IdAndBook_Id(Long userId, Long bookId);

    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Review> findAllByUser_IdOrderByUpdatedAtDesc(Long userId);

    void deleteAllByBook_Id(Long bookId);
}
