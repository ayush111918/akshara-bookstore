package com.akshara.api.book.entity;

import com.akshara.api.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    @Test
    void decreaseStockShouldReduceAvailableQuantity() {
        Inventory inventory = createInventory(
                10,
                AvailabilityStatus.IN_STOCK
        );

        inventory.decreaseStock(3);

        assertEquals(7, inventory.getStockQuantity());
        assertEquals(
                AvailabilityStatus.IN_STOCK,
                inventory.getAvailabilityStatus()
        );
    }

    @Test
    void decreaseStockShouldMarkInventoryOutOfStockWhenQuantityReachesZero() {
        Inventory inventory = createInventory(
                5,
                AvailabilityStatus.IN_STOCK
        );

        inventory.decreaseStock(5);

        assertEquals(0, inventory.getStockQuantity());
        assertEquals(
                AvailabilityStatus.OUT_OF_STOCK,
                inventory.getAvailabilityStatus()
        );
    }

    @Test
    void decreaseStockShouldRejectZeroQuantity() {
        Inventory inventory = createInventory(
                10,
                AvailabilityStatus.IN_STOCK
        );

        assertThrows(
                InvalidRequestException.class,
                () -> inventory.decreaseStock(0)
        );

        assertEquals(10, inventory.getStockQuantity());
    }

    @Test
    void decreaseStockShouldRejectNegativeQuantity() {
        Inventory inventory = createInventory(
                10,
                AvailabilityStatus.IN_STOCK
        );

        assertThrows(
                InvalidRequestException.class,
                () -> inventory.decreaseStock(-2)
        );

        assertEquals(10, inventory.getStockQuantity());
    }

    @Test
    void decreaseStockShouldRejectInactiveInventory() {
        Inventory inventory = createInventory(
                10,
                AvailabilityStatus.IN_STOCK
        );
        inventory.setActive(false);

        assertThrows(
                InvalidRequestException.class,
                () -> inventory.decreaseStock(1)
        );

        assertEquals(10, inventory.getStockQuantity());
    }

    @Test
    void decreaseStockShouldRejectUnavailableInventory() {
        Inventory inventory = createInventory(
                10,
                AvailabilityStatus.UNAVAILABLE
        );

        assertThrows(
                InvalidRequestException.class,
                () -> inventory.decreaseStock(1)
        );

        assertEquals(10, inventory.getStockQuantity());
    }

    @Test
    void decreaseStockShouldRejectQuantityGreaterThanAvailableStock() {
        Inventory inventory = createInventory(
                3,
                AvailabilityStatus.IN_STOCK
        );

        assertThrows(
                InvalidRequestException.class,
                () -> inventory.decreaseStock(4)
        );

        assertEquals(3, inventory.getStockQuantity());
        assertEquals(
                AvailabilityStatus.IN_STOCK,
                inventory.getAvailabilityStatus()
        );
    }

    private Inventory createInventory(
            int stockQuantity,
            AvailabilityStatus availabilityStatus
    ) {
        return new Inventory(
                null,
                new BigDecimal("499.00"),
                stockQuantity,
                availabilityStatus
        );
    }
}