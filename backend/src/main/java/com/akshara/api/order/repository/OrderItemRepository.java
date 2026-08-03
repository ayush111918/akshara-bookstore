package com.akshara.api.order.repository;

import com.akshara.api.order.entity.OrderItem;
import com.akshara.api.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrder_IdOrderByIdAsc(
            Long orderId
    );

    @EntityGraph(attributePaths = {
            "order",
            "bookEdition",
            "bookEdition.book"
    })
    List<OrderItem> findAllByOrder_User_IdAndOrder_StatusNotOrderByOrder_PlacedAtDescIdDesc(
            Long userId,
            OrderStatus excludedStatus
    );

    @EntityGraph(attributePaths = {
            "order",
            "bookEdition",
            "bookEdition.book"
    })
    Optional<OrderItem> findFirstByOrder_User_IdAndOrder_StatusAndBookEdition_Book_IdOrderByOrder_PlacedAtDescIdDesc(
            Long userId,
            OrderStatus status,
            Long bookId
    );

    boolean existsByOrder_User_IdAndOrder_StatusAndBookEdition_Book_Id(
            Long userId,
            OrderStatus status,
            Long bookId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OrderItem item
            set item.bookEdition = null
            where item.bookEdition.book.id = :bookId
            """)
    int detachAllByBookId(@Param("bookId") Long bookId);
}
