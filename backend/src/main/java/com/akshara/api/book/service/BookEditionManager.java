package com.akshara.api.book.service;

import com.akshara.api.book.dto.BookEditionRequest;
import com.akshara.api.book.dto.InventoryRequest;
import com.akshara.api.book.entity.Book;
import com.akshara.api.book.entity.BookEdition;
import com.akshara.api.book.entity.Inventory;
import com.akshara.api.book.entity.Publisher;
import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.book.repository.InventoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
class BookEditionManager {

    private final BookEditionRepository editions;
    private final InventoryRepository inventories;
    private final BookRequestValidator validator;

    BookEditionManager(
            BookEditionRepository editions,
            InventoryRepository inventories,
            BookRequestValidator validator
    ) {
        this.editions = editions;
        this.inventories = inventories;
        this.validator = validator;
    }

    void create(
            Book book,
            List<BookEditionRequest> requests,
            Map<Long, Publisher> publishers,
            String externalEditionId
    ) {
        for (BookEditionRequest request : requests) {
            BookEdition edition = new BookEdition(book, request.format());
            if (request.publisherId() != null) {
                edition.setPublisher(publishers.get(request.publisherId()));
            }
            edition.setEditionName(validator.normalizeOptional(request.editionName()));
            edition.setIsbn10(validator.normalizeIsbn10(request.isbn10()));
            edition.setIsbn13(validator.normalizeIsbn13(request.isbn13()));
            edition.setPublicationDate(request.publicationDate());
            edition.setPageCount(request.pageCount());
            edition.setSku(request.sku().trim());
            edition.setExternalEditionId(externalEditionId);

            BookEdition savedEdition = editions.save(edition);
            InventoryRequest inventoryRequest = request.inventory();
            Inventory inventory = new Inventory(
                    savedEdition, inventoryRequest.price(), inventoryRequest.stockQuantity(),
                    inventoryRequest.availabilityStatus()
            );
            inventory.setActive(inventoryRequest.active());
            inventories.save(inventory);
        }
    }

    void replace(
            Book book,
            List<BookEditionRequest> requests,
            Map<Long, Publisher> publishers
    ) {
        deleteForBook(book.getId());
        create(book, requests, publishers, null);
    }

    void deleteForBook(Long bookId) {
        List<BookEdition> existing = editions.findAllByBook_Id(bookId);
        List<Inventory> linkedInventories = existing.stream()
                .map(edition -> inventories.findByBookEdition_Id(edition.getId()))
                .flatMap(optional -> optional.stream()).toList();
        inventories.deleteAll(linkedInventories);
        inventories.flush();
        editions.deleteAll(existing);
        editions.flush();
    }
}
