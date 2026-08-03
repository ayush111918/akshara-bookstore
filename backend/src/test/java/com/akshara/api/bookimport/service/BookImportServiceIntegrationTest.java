package com.akshara.api.bookimport.service;

import com.akshara.api.book.dto.BookResponse;
import com.akshara.api.book.dto.InventoryRequest;
import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.book.repository.AuthorRepository;
import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.book.repository.BookRepository;
import com.akshara.api.book.repository.CategoryRepository;
import com.akshara.api.book.repository.InventoryRepository;
import com.akshara.api.book.repository.PublisherRepository;
import com.akshara.api.book.service.BookService;
import com.akshara.api.cart.entity.Cart;
import com.akshara.api.cart.entity.CartItem;
import com.akshara.api.cart.repository.CartItemRepository;
import com.akshara.api.cart.repository.CartRepository;
import com.akshara.api.bookimport.dto.BookImportRequest;
import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.ManualBookRequest;
import com.akshara.api.common.exception.DuplicateResourceException;
import com.akshara.api.common.exception.InvalidRequestException;
import com.akshara.api.review.entity.Review;
import com.akshara.api.review.repository.ReviewRepository;
import com.akshara.api.order.entity.Address;
import com.akshara.api.order.entity.Order;
import com.akshara.api.order.entity.OrderItem;
import com.akshara.api.order.repository.OrderItemRepository;
import com.akshara.api.order.repository.OrderRepository;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.entity.Role;
import com.akshara.api.user.repository.UserRepository;
import com.akshara.api.wishlist.entity.WishlistItem;
import com.akshara.api.wishlist.repository.WishlistItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:import_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=01234567890123456789012345678901"
})
@Transactional
class BookImportServiceIntegrationTest {

    @Autowired private BookImportService service;
    @Autowired private BookService bookService;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookEditionRepository editionRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private AuthorRepository authorRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PublisherRepository publisherRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private WishlistItemRepository wishlistItemRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    @Test
    void importPersistsCompleteGraphAndNormalizesZeroStock() {
        BookResponse response = service.importBook(importRequest(
                "978-0-7352-1129-2", "AKS-ATOMIC-PB", "/books/OL1M"
        ));

        assertNotNull(response.id());
        assertEquals(1, bookRepository.count());
        assertEquals(1, editionRepository.count());
        assertEquals(1, inventoryRepository.count());
        assertEquals(1, authorRepository.count());
        assertEquals(1, categoryRepository.count());
        assertEquals(1, publisherRepository.count());
        assertEquals("9780735211292", response.editions().get(0).isbn13());
        assertEquals(AvailabilityStatus.OUT_OF_STOCK,
                response.editions().get(0).inventory().availabilityStatus());
    }

    @Test
    void secondEditionFromSameWorkReusesConceptualBookAndNames() {
        BookResponse first = service.importBook(importRequest(
                "9780735211292", "AKS-ATOMIC-PB", "/books/OL1M"
        ));
        BookResponse second = service.importBook(importRequest(
                "9780735211308", "AKS-ATOMIC-HB", "/books/OL2M"
        ));

        assertEquals(first.id(), second.id());
        assertEquals(1, bookRepository.count());
        assertEquals(2, editionRepository.count());
        assertEquals(1, authorRepository.count());
        assertEquals(1, categoryRepository.count());
        assertEquals(1, publisherRepository.count());
        assertEquals(2, second.editions().size());
    }

    @Test
    void duplicateNormalizedIsbnAndSkuAreRejected() {
        service.importBook(importRequest(
                "9780735211292", "AKS-ATOMIC-PB", "/books/OL1M"
        ));

        assertThrows(DuplicateResourceException.class, () ->
                service.importBook(importRequest(
                        "978-0-7352-1129-2", "AKS-OTHER", "/books/OL3M"
                ))
        );
        assertThrows(DuplicateResourceException.class, () ->
                service.importBook(importRequest(
                        "9780735211308", "aks-atomic-pb", "/books/OL4M"
                ))
        );
    }

    @Test
    void manualEntryCanStoreRareBookWithoutIsbn() {
        BookResponse response = service.createManualBook(new ManualBookRequest(
                "Local Oral Histories", null, "A local publication",
                List.of("A. Archivist"), "Akshara Local Press", "en", 80,
                List.of("Local History"), null, LocalDate.of(2020, 1, 1),
                "First edition", null, null, BookFormat.PAPERBACK,
                new BigDecimal("199.00"), 3, AvailabilityStatus.IN_STOCK,
                "AKS-LOCAL-001", true, false
        ));

        assertNotNull(response.id());
        assertEquals("AKS-LOCAL-001", response.editions().get(0).sku());
    }

    @Test
    void activeDigitalEditionIsRejectedUntilDeliveryIsConfigured() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.createManualBook(new ManualBookRequest(
                        "Licensed eBook", null, "Not yet deliverable",
                        List.of("Digital Author"), "Digital Press", "en", 120,
                        List.of("Technology"), null, LocalDate.of(2024, 1, 1),
                        "EPUB edition", null, null, BookFormat.EPUB,
                        new BigDecimal("199.00"), 1, AvailabilityStatus.IN_STOCK,
                        "AKS-DIGITAL-001", true, false
                ))
        );

        assertEquals(
                "Digital editions cannot be activated for sale until "
                        + "a licensed file and reader entitlement are configured",
                exception.getMessage()
        );
        assertEquals(0, bookRepository.count());
    }

    @Test
    void restockAddsUnitsAndMakesAnOutOfStockEditionAvailable() {
        BookResponse imported = service.importBook(importRequest(
                "9780735211292", "AKS-ATOMIC-PB", "/books/OL1M"
        ));
        Long editionId = imported.editions().get(0).id();

        var inventory = bookService.restockInventory(editionId, 15);

        assertEquals(15, inventory.stockQuantity());
        assertEquals(AvailabilityStatus.IN_STOCK, inventory.availabilityStatus());
    }

    @Test
    void inventoryUpdateNormalizesImpossibleZeroInStockCombination() {
        BookResponse imported = service.importBook(importRequest(
                "9780735211292", "AKS-ATOMIC-PB", "/books/OL1M"
        ));
        Long editionId = imported.editions().get(0).id();

        var inventory = bookService.updateInventory(
                editionId,
                new InventoryRequest(
                        new BigDecimal("525.00"), 0,
                        AvailabilityStatus.IN_STOCK, false
                )
        );

        assertEquals(new BigDecimal("525.00"), inventory.price());
        assertEquals(0, inventory.stockQuantity());
        assertEquals(AvailabilityStatus.OUT_OF_STOCK, inventory.availabilityStatus());
        assertEquals(false, inventory.active());
    }

    @Test
    void deletionCleansDisposableReaderReferences() {
        BookResponse imported = service.importBook(importRequest(
                "9780735211292", "AKS-ATOMIC-PB", "/books/OL1M"
        ));
        var book = bookRepository.findById(imported.id()).orElseThrow();
        var edition = editionRepository.findById(
                imported.editions().get(0).id()
        ).orElseThrow();
        AppUser reader = userRepository.save(new AppUser(
                "Reader One", "reader-one@example.com",
                "encoded-password", Role.READER
        ));
        Cart cart = cartRepository.save(new Cart(reader));
        cartItemRepository.save(new CartItem(cart, edition, 1));
        wishlistItemRepository.save(new WishlistItem(reader, book));
        reviewRepository.save(new Review(
                reader, book, 5, "Excellent", "A useful book."
        ));
        Order order = orderRepository.save(new Order(
                reader,
                new Address(
                        "Reader One", "9999999999", "1 Book Lane", null,
                        "Pune", "Maharashtra", "411001", "India"
                ),
                new BigDecimal("299.00"), BigDecimal.ZERO
        ));
        orderItemRepository.save(new OrderItem(
                order, edition, new BigDecimal("299.00"), 1
        ));

        bookService.delete(imported.id());

        assertFalse(bookRepository.existsById(imported.id()));
        assertEquals(0, cartItemRepository.count());
        assertEquals(0, wishlistItemRepository.count());
        assertEquals(0, reviewRepository.count());
        assertEquals(1, cartRepository.count());
        assertEquals(1, userRepository.count());
        OrderItem historicalItem = orderItemRepository
                .findAllByOrder_IdOrderByIdAsc(order.getId())
                .get(0);
        assertEquals(null, historicalItem.getBookEdition());
        assertEquals("Atomic Habits", historicalItem.getBookTitle());
        assertEquals(1, orderRepository.count());
    }

    private BookImportRequest importRequest(
            String isbn13,
            String sku,
            String editionId
    ) {
        return new BookImportRequest(
                BookImportSource.OPEN_LIBRARY,
                "/works/OL17930368W",
                editionId,
                "Atomic Habits",
                null,
                "Tiny changes, remarkable results.",
                List.of("James Clear"),
                "Avery",
                "en",
                320,
                List.of("Self-help"),
                "https://covers.openlibrary.org/b/id/1-L.jpg",
                LocalDate.of(2018, 10, 16),
                "First edition",
                null,
                isbn13,
                sku.endsWith("HB") ? BookFormat.HARDCOVER : BookFormat.PAPERBACK,
                new BigDecimal("499.00"),
                0,
                AvailabilityStatus.IN_STOCK,
                sku,
                true,
                true
        );
    }
}
