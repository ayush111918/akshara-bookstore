package com.akshara.api.cart.repository;

import com.akshara.api.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository
        extends JpaRepository<CartItem, Long> {

    List<CartItem> findAllByCart_IdOrderByCreatedAtAsc(
            Long cartId
    );

    Optional<CartItem> findByCart_IdAndBookEdition_Id(
            Long cartId,
            Long bookEditionId
    );

    Optional<CartItem> findByIdAndCart_Id(
            Long itemId,
            Long cartId
    );

    boolean existsByCart_IdAndBookEdition_Id(
            Long cartId,
            Long bookEditionId
    );

    void deleteAllByCart_Id(Long cartId);
}