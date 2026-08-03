package com.akshara.api.bookimport.service;

import com.akshara.api.book.dto.BookEditionRequest;
import com.akshara.api.book.dto.BookRequest;
import com.akshara.api.book.dto.BookResponse;
import com.akshara.api.book.dto.InventoryRequest;
import com.akshara.api.book.entity.Author;
import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.Category;
import com.akshara.api.book.entity.Publisher;
import com.akshara.api.book.repository.AuthorRepository;
import com.akshara.api.book.repository.CategoryRepository;
import com.akshara.api.book.repository.PublisherRepository;
import com.akshara.api.book.service.BookService;
import com.akshara.api.bookimport.dto.BookImportRequest;
import com.akshara.api.bookimport.dto.CatalogueBookCommand;
import com.akshara.api.bookimport.dto.ManualBookRequest;
import com.akshara.api.common.exception.InvalidRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class BookImportService {

    private final BookService bookService;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;

    public BookImportService(
            BookService bookService,
            AuthorRepository authorRepository,
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository
    ) {
        this.bookService = bookService;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
    }

    @Transactional
    public BookResponse importBook(BookImportRequest request) {
        return save(
                request, request.source().name(), request.sourceId(),
                request.editionId(), true
        );
    }

    @Transactional
    public BookResponse createManualBook(ManualBookRequest request) {
        return save(request, null, null, null, false);
    }

    private BookResponse save(
            CatalogueBookCommand request,
            String metadataSource,
            String externalWorkId,
            String externalEditionId,
            boolean isbnRequired
    ) {
        if (isbnRequired && blank(request.isbn10()) && blank(request.isbn13())) {
            throw new InvalidRequestException(
                    "An ISBN-10 or ISBN-13 is required to import an edition"
            );
        }

        Set<Long> authorIds = new LinkedHashSet<>();
        request.authors().stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .map(this::findOrCreateAuthor)
                .map(Author::getId)
                .forEach(authorIds::add);

        Set<Long> categoryIds = new LinkedHashSet<>();
        request.categories().stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .map(this::findOrCreateCategory)
                .map(Category::getId)
                .forEach(categoryIds::add);

        Long publisherId = blank(request.publisher())
                ? null
                : findOrCreatePublisher(request.publisher().trim()).getId();

        InventoryRequest inventory = new InventoryRequest(
                request.price(),
                request.stockQuantity(),
                normalizedAvailability(
                        request.stockQuantity(), request.availabilityStatus()
                ),
                request.active()
        );
        BookEditionRequest edition = new BookEditionRequest(
                publisherId,
                request.format(),
                request.editionName(),
                request.isbn10(),
                request.isbn13(),
                request.publicationDate(),
                request.pageCount(),
                request.sku(),
                inventory
        );
        BookRequest book = new BookRequest(
                request.title(),
                request.subtitle(),
                request.description(),
                request.coverImageUrl(),
                request.languageCode(),
                request.featured(),
                authorIds,
                categoryIds,
                java.util.List.of(edition)
        );
        return metadataSource == null
                ? bookService.create(book)
                : bookService.createImported(
                        book, metadataSource, externalWorkId, externalEditionId
                );
    }

    private Author findOrCreateAuthor(String name) {
        return authorRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> authorRepository.save(new Author(name)));
    }

    private Publisher findOrCreatePublisher(String name) {
        return publisherRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> publisherRepository.save(new Publisher(name)));
    }

    private Category findOrCreateCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.save(
                        new Category(name, uniqueSlug(name))
                ));
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "category";
        if (base.length() > 110) base = base.substring(0, 110).replaceAll("-$", "");
        String candidate = base;
        int suffix = 2;
        while (categoryRepository.existsBySlugIgnoreCase(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private AvailabilityStatus normalizedAvailability(
            int stock,
            AvailabilityStatus requested
    ) {
        return stock == 0 && requested == AvailabilityStatus.IN_STOCK
                ? AvailabilityStatus.OUT_OF_STOCK
                : requested;
    }
}
