package com.akshara.api.book.repository;

import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository
        extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByBookEdition_Id(Long bookEditionId);

    boolean existsByBookEdition_Id(Long bookEditionId);

    List<Inventory> findAllByActiveTrue();

    List<Inventory> findAllByActiveTrueAndAvailabilityStatus(
            AvailabilityStatus availabilityStatus
    );
}