package com.akshara.api.order.repository;

import com.akshara.api.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}