package com.akshara.api.cart.repository;

import com.akshara.api.cart.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser_Id(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cart
            from Cart cart
            where cart.user.id = :userId
            """)
    Optional<Cart> findByUserIdForUpdate(
            @Param("userId") Long userId
    );

    boolean existsByUser_Id(Long userId);
}