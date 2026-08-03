package com.akshara.api.order.repository;

import com.akshara.api.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "shippingAddress")
    List<Order> findAllByUser_IdOrderByPlacedAtDesc(
            Long userId
    );

    @EntityGraph(attributePaths = "shippingAddress")
    Optional<Order> findByIdAndUser_Id(
            Long orderId,
            Long userId
    );

    @EntityGraph(attributePaths = {
            "user",
            "shippingAddress"
    })
    List<Order> findAllByOrderByPlacedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "user",
            "shippingAddress"
    })
    @Query("""
            select orders
            from Order orders
            where orders.id = :orderId
            """)
    Optional<Order> findByIdForUpdate(
            @Param("orderId") Long orderId
    );
}