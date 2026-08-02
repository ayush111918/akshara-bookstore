package com.akshara.api.wishlist.repository;

import com.akshara.api.wishlist.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<WishlistItem> findByUser_IdAndBook_Id(Long userId, Long bookId);
}
