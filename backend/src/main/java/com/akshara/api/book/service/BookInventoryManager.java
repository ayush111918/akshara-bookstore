package com.akshara.api.book.service;

import com.akshara.api.book.dto.InventoryRequest;
import com.akshara.api.book.dto.InventoryResponse;
import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.Inventory;
import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.book.repository.InventoryRepository;
import com.akshara.api.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
class BookInventoryManager {

    private final BookEditionRepository editions;
    private final InventoryRepository inventories;
    private final BookRequestValidator validator;

    BookInventoryManager(
            BookEditionRepository editions,
            InventoryRepository inventories,
            BookRequestValidator validator
    ) {
        this.editions = editions;
        this.inventories = inventories;
        this.validator = validator;
    }

    InventoryResponse update(Long editionId, InventoryRequest request) {
        Inventory inventory = findForUpdate(editionId);
        validator.rejectUnsupportedDigitalSale(inventory.getBookEdition().getFormat(), request.active());
        inventory.setPrice(request.price());
        inventory.setStockQuantity(request.stockQuantity());
        inventory.setAvailabilityStatus(normalizeAvailability(
                request.stockQuantity(), request.availabilityStatus()
        ));
        inventory.setActive(request.active());
        return response(inventories.save(inventory));
    }

    InventoryResponse restock(Long editionId, int quantity) {
        Inventory inventory = findForUpdate(editionId);
        inventory.increaseStock(quantity);
        return response(inventories.save(inventory));
    }

    private Inventory findForUpdate(Long editionId) {
        if (!editions.existsById(editionId)) {
            throw new ResourceNotFoundException("Book edition not found with id: " + editionId);
        }
        return inventories.findByBookEditionIdForUpdate(editionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for book edition id: " + editionId
                ));
    }

    private AvailabilityStatus normalizeAvailability(int stock, AvailabilityStatus requested) {
        return stock == 0 && requested == AvailabilityStatus.IN_STOCK
                ? AvailabilityStatus.OUT_OF_STOCK : requested;
    }

    private InventoryResponse response(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(), inventory.getPrice(), inventory.getStockQuantity(),
                inventory.getAvailabilityStatus(), inventory.isActive(), inventory.getUpdatedAt()
        );
    }
}
