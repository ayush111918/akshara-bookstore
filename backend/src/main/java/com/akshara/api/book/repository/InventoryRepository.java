package com.akshara.api.book.repository;

import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository
        extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByBookEdition_Id(
            Long bookEditionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select inventory
            from Inventory inventory
            where inventory.bookEdition.id = :bookEditionId
            """)
    Optional<Inventory> findByBookEditionIdForUpdate(
            @Param("bookEditionId") Long bookEditionId
    );

    boolean existsByBookEdition_Id(Long bookEditionId);

    List<Inventory> findAllByActiveTrue();

    List<Inventory> findAllByActiveTrueAndAvailabilityStatus(
            AvailabilityStatus availabilityStatus
    );
}