package com.akshara.api.book.service;

import com.akshara.api.book.dto.AuthorResponse;
import com.akshara.api.book.dto.BookEditionResponse;
import com.akshara.api.book.dto.BookResponse;
import com.akshara.api.book.dto.CategoryResponse;
import com.akshara.api.book.dto.InventoryResponse;
import com.akshara.api.book.dto.PublisherResponse;
import com.akshara.api.book.entity.Author;
import com.akshara.api.book.entity.Book;
import com.akshara.api.book.entity.BookAuthor;
import com.akshara.api.book.entity.BookCategory;
import com.akshara.api.book.entity.BookEdition;
import com.akshara.api.book.entity.Category;
import com.akshara.api.book.entity.Inventory;
import com.akshara.api.book.entity.Publisher;
import com.akshara.api.book.repository.BookAuthorRepository;
import com.akshara.api.book.repository.BookCategoryRepository;
import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.book.repository.InventoryRepository;
import com.akshara.api.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
class BookResponseMapper {

    private final BookAuthorRepository authorLinks;
    private final BookCategoryRepository categoryLinks;
    private final BookEditionRepository editions;
    private final InventoryRepository inventories;

    BookResponseMapper(
            BookAuthorRepository authorLinks,
            BookCategoryRepository categoryLinks,
            BookEditionRepository editions,
            InventoryRepository inventories
    ) {
        this.authorLinks = authorLinks;
        this.categoryLinks = categoryLinks;
        this.editions = editions;
        this.inventories = inventories;
    }

    BookResponse response(Book book) {
        List<AuthorResponse> authors = authorLinks.findAllByBook_Id(book.getId()).stream()
                .map(BookAuthor::getAuthor)
                .sorted(Comparator.comparing(Author::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::author).toList();
        List<CategoryResponse> categories = categoryLinks.findAllByBook_Id(book.getId()).stream()
                .map(BookCategory::getCategory)
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::category).toList();
        List<BookEditionResponse> editionResponses = editions.findAllByBook_Id(book.getId()).stream()
                .sorted(Comparator.comparing(BookEdition::getId))
                .map(this::edition).toList();

        return new BookResponse(
                book.getId(), book.getTitle(), book.getSubtitle(), book.getDescription(),
                book.getCoverImageUrl(), book.getLanguageCode(), book.isFeatured(),
                book.getMetadataSource(), book.getExternalWorkId(), authors, categories,
                editionResponses, book.getCreatedAt(), book.getUpdatedAt()
        );
    }

    private BookEditionResponse edition(BookEdition edition) {
        PublisherResponse publisher = edition.getPublisher() == null
                ? null : publisher(edition.getPublisher());
        Inventory inventory = inventories.findByBookEdition_Id(edition.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for book edition id: " + edition.getId()
                ));
        return new BookEditionResponse(
                edition.getId(), publisher, edition.getFormat(), edition.getEditionName(),
                edition.getIsbn10(), edition.getIsbn13(), edition.getPublicationDate(),
                edition.getPageCount(), edition.getSku(), edition.getExternalEditionId(),
                inventory(inventory), edition.getCreatedAt(), edition.getUpdatedAt()
        );
    }

    private AuthorResponse author(Author author) {
        return new AuthorResponse(
                author.getId(), author.getName(), author.getBiography(),
                author.getCreatedAt(), author.getUpdatedAt()
        );
    }

    private CategoryResponse category(Category category) {
        return new CategoryResponse(
                category.getId(), category.getName(), category.getSlug(), category.getDescription(),
                category.getCreatedAt(), category.getUpdatedAt()
        );
    }

    private PublisherResponse publisher(Publisher publisher) {
        return new PublisherResponse(
                publisher.getId(), publisher.getName(), publisher.getWebsiteUrl(),
                publisher.getCreatedAt(), publisher.getUpdatedAt()
        );
    }

    private InventoryResponse inventory(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(), inventory.getPrice(), inventory.getStockQuantity(),
                inventory.getAvailabilityStatus(), inventory.isActive(), inventory.getUpdatedAt()
        );
    }
}
