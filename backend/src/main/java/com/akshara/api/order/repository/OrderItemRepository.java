package com.akshara.api.order.repository;

import com.akshara.api.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrder_IdOrderByIdAsc(
            Long orderId
    );
}