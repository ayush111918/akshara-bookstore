package com.akshara.api.book.service;

import com.akshara.api.book.dto.AuthorResponse;
import com.akshara.api.book.dto.BookEditionRequest;
import com.akshara.api.book.dto.BookEditionResponse;
import com.akshara.api.book.dto.BookRequest;
import com.akshara.api.book.dto.BookResponse;
import com.akshara.api.book.dto.CategoryResponse;
import com.akshara.api.book.dto.InventoryRequest;
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
import com.akshara.api.book.repository.AuthorRepository;
import com.akshara.api.book.repository.BookAuthorRepository;
import com.akshara.api.book.repository.BookCategoryRepository;
import com.akshara.api.book.repository.BookEditionRepository;
import com.akshara.api.book.repository.BookRepository;
import com.akshara.api.book.repository.CategoryRepository;
import com.akshara.api.book.repository.PublisherRepository;
import com.akshara.api.common.exception.DuplicateResourceException;
import com.akshara.api.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.akshara.api.book.dto.BookPageResponse;
import com.akshara.api.book.dto.BookSearchCriteria;
import com.akshara.api.book.dto.BookSort;
import com.akshara.api.book.specification.BookSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.akshara.api.book.repository.InventoryRepository;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final BookEditionRepository bookEditionRepository;
    private final InventoryRepository inventoryRepository;

    public BookService(
            BookRepository bookRepository,
            AuthorRepository authorRepository,
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository,
            BookAuthorRepository bookAuthorRepository,
            BookCategoryRepository bookCategoryRepository,
            BookEditionRepository bookEditionRepository,
            InventoryRepository inventoryRepository
    ) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.bookAuthorRepository = bookAuthorRepository;
        this.bookCategoryRepository = bookCategoryRepository;
        this.bookEditionRepository = bookEditionRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        List<Author> authors = findAuthors(request.authorIds());
        List<Category> categories = findCategories(
                request.categoryIds()
        );

        Map<Long, Publisher> publishers = findPublishers(
                request.editions()
        );

        validateIsbnUniqueness(request.editions(), null);

        Book book = new Book(request.title().trim());
        applyBookFields(book, request);

        Book savedBook = bookRepository.save(book);

        saveAuthorRelationships(savedBook, authors);
        saveCategoryRelationships(savedBook, categories);
        createEditions(savedBook, request.editions(), publishers);

        return toResponse(savedBook);
    }

    public List<BookResponse> getAll() {
        return bookRepository
                .findAll(
                        Sort.by(Sort.Direction.ASC, "title")
                                .and(Sort.by(
                                        Sort.Direction.ASC,
                                        "id"
                                ))
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BookPageResponse search(
            BookSearchCriteria criteria,
            int page,
            int size,
            BookSort sort
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                createSort(sort)
        );

        Page<Book> result = bookRepository.findAll(
                BookSpecifications.withCriteria(criteria),
                pageable
        );

        List<BookResponse> content = result.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new BookPageResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    public BookResponse getById(Long id) {
        return toResponse(findBookById(id));
    }

    @Transactional
    public BookResponse update(
            Long id,
            BookRequest request
    ) {
        Book book = findBookById(id);

        List<Author> authors = findAuthors(request.authorIds());
        List<Category> categories = findCategories(
                request.categoryIds()
        );

        Map<Long, Publisher> publishers = findPublishers(
                request.editions()
        );

        validateIsbnUniqueness(request.editions(), id);

        applyBookFields(book, request);
        bookRepository.save(book);

        replaceAuthorRelationships(book, authors);
        replaceCategoryRelationships(book, categories);
        replaceEditions(book, request.editions(), publishers);

        return toResponse(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = findBookById(id);

        List<BookAuthor> authorLinks =
                bookAuthorRepository.findAllByBook_Id(id);

        List<BookCategory> categoryLinks =
                bookCategoryRepository.findAllByBook_Id(id);

        List<BookEdition> editions =
                bookEditionRepository.findAllByBook_Id(id);

        deleteEditionInventory(editions);

        bookEditionRepository.deleteAll(editions);
        bookEditionRepository.flush();

        bookAuthorRepository.deleteAll(authorLinks);
        bookAuthorRepository.flush();

        bookCategoryRepository.deleteAll(categoryLinks);
        bookCategoryRepository.flush();

        bookRepository.delete(book);
    }

    private Book findBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book not found with id: " + id
                ));
    }

    private List<Author> findAuthors(Set<Long> authorIds) {
        List<Author> authors = authorRepository.findAllById(authorIds);

        Set<Long> foundIds = authors.stream()
                .map(Author::getId)
                .collect(Collectors.toSet());

        List<Long> missingIds = authorIds.stream()
                .filter(id -> !foundIds.contains(id))
                .sorted()
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Authors not found with ids: " + missingIds
            );
        }

        return authors;
    }

    private List<Category> findCategories(Set<Long> categoryIds) {
        List<Category> categories =
                categoryRepository.findAllById(categoryIds);

        Set<Long> foundIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        List<Long> missingIds = categoryIds.stream()
                .filter(id -> !foundIds.contains(id))
                .sorted()
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Categories not found with ids: " + missingIds
            );
        }

        return categories;
    }

    private Map<Long, Publisher> findPublishers(
            List<BookEditionRequest> editionRequests
    ) {
        Set<Long> publisherIds = editionRequests.stream()
                .map(BookEditionRequest::publisherId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<Publisher> publishers =
                publisherRepository.findAllById(publisherIds);

        Set<Long> foundIds = publishers.stream()
                .map(Publisher::getId)
                .collect(Collectors.toSet());

        List<Long> missingIds = publisherIds.stream()
                .filter(id -> !foundIds.contains(id))
                .sorted()
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Publishers not found with ids: " + missingIds
            );
        }

        return publishers.stream()
                .collect(Collectors.toMap(
                        Publisher::getId,
                        Function.identity()
                ));
    }

    private void validateIsbnUniqueness(
            List<BookEditionRequest> editions,
            Long currentBookId
    ) {
        Set<String> requestIsbn10Values = new HashSet<>();
        Set<String> requestIsbn13Values = new HashSet<>();

        for (BookEditionRequest edition : editions) {
            String isbn10 = normalizeIsbn10(edition.isbn10());
            String isbn13 = normalizeOptional(edition.isbn13());

            validateIsbn10(
                    isbn10,
                    currentBookId,
                    requestIsbn10Values
            );

            validateIsbn13(
                    isbn13,
                    currentBookId,
                    requestIsbn13Values
            );
        }
    }

    private void validateIsbn10(
            String isbn10,
            Long currentBookId,
            Set<String> requestValues
    ) {
        if (isbn10 == null) {
            return;
        }

        if (!requestValues.add(isbn10)) {
            throw new DuplicateResourceException(
                    "ISBN-10 '" + isbn10
                            + "' occurs more than once in the request"
            );
        }

        bookEditionRepository.findByIsbn10(isbn10)
                .filter(existing ->
                        currentBookId == null
                                || !existing.getBook()
                                .getId()
                                .equals(currentBookId)
                )
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A book edition with ISBN-10 '"
                                    + isbn10 + "' already exists"
                    );
                });
    }

    private void validateIsbn13(
            String isbn13,
            Long currentBookId,
            Set<String> requestValues
    ) {
        if (isbn13 == null) {
            return;
        }

        if (!requestValues.add(isbn13)) {
            throw new DuplicateResourceException(
                    "ISBN-13 '" + isbn13
                            + "' occurs more than once in the request"
            );
        }

        bookEditionRepository.findByIsbn13(isbn13)
                .filter(existing ->
                        currentBookId == null
                                || !existing.getBook()
                                .getId()
                                .equals(currentBookId)
                )
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A book edition with ISBN-13 '"
                                    + isbn13 + "' already exists"
                    );
                });
    }

    private void applyBookFields(
            Book book,
            BookRequest request
    ) {
        book.setTitle(request.title().trim());
        book.setSubtitle(normalizeOptional(request.subtitle()));
        book.setDescription(normalizeOptional(request.description()));
        book.setCoverImageUrl(
                normalizeOptional(request.coverImageUrl())
        );
        book.setLanguageCode(
                normalizeOptional(request.languageCode())
        );
    }

    private void saveAuthorRelationships(
            Book book,
            List<Author> authors
    ) {
        List<BookAuthor> relationships = authors.stream()
                .map(author -> new BookAuthor(book, author))
                .toList();

        bookAuthorRepository.saveAll(relationships);
    }

    private void saveCategoryRelationships(
            Book book,
            List<Category> categories
    ) {
        List<BookCategory> relationships = categories.stream()
                .map(category -> new BookCategory(book, category))
                .toList();

        bookCategoryRepository.saveAll(relationships);
    }

    private void replaceAuthorRelationships(
            Book book,
            List<Author> requestedAuthors
    ) {
        List<BookAuthor> existingLinks =
                bookAuthorRepository.findAllByBook_Id(book.getId());

        Set<Long> requestedIds = requestedAuthors.stream()
                .map(Author::getId)
                .collect(Collectors.toSet());

        Set<Long> existingIds = existingLinks.stream()
                .map(link -> link.getAuthor().getId())
                .collect(Collectors.toSet());

        List<BookAuthor> linksToDelete = existingLinks.stream()
                .filter(link ->
                        !requestedIds.contains(
                                link.getAuthor().getId()
                        )
                )
                .toList();

        List<BookAuthor> linksToAdd = requestedAuthors.stream()
                .filter(author ->
                        !existingIds.contains(author.getId())
                )
                .map(author -> new BookAuthor(book, author))
                .toList();

        bookAuthorRepository.deleteAll(linksToDelete);
        bookAuthorRepository.saveAll(linksToAdd);
    }

    private void replaceCategoryRelationships(
            Book book,
            List<Category> requestedCategories
    ) {
        List<BookCategory> existingLinks =
                bookCategoryRepository.findAllByBook_Id(book.getId());

        Set<Long> requestedIds = requestedCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        Set<Long> existingIds = existingLinks.stream()
                .map(link -> link.getCategory().getId())
                .collect(Collectors.toSet());

        List<BookCategory> linksToDelete = existingLinks.stream()
                .filter(link ->
                        !requestedIds.contains(
                                link.getCategory().getId()
                        )
                )
                .toList();

        List<BookCategory> linksToAdd =
                requestedCategories.stream()
                        .filter(category ->
                                !existingIds.contains(category.getId())
                        )
                        .map(category ->
                                new BookCategory(book, category)
                        )
                        .toList();

        bookCategoryRepository.deleteAll(linksToDelete);
        bookCategoryRepository.saveAll(linksToAdd);
    }

    private void replaceEditions(
            Book book,
            List<BookEditionRequest> editionRequests,
            Map<Long, Publisher> publishers
    ) {
        List<BookEdition> existingEditions =
                bookEditionRepository.findAllByBook_Id(book.getId());

        deleteEditionInventory(existingEditions);

        bookEditionRepository.deleteAll(existingEditions);
        bookEditionRepository.flush();

        createEditions(book, editionRequests, publishers);
    }

    private void deleteEditionInventory(
            List<BookEdition> editions
    ) {
        List<Inventory> inventories = editions.stream()
                .map(edition ->
                        inventoryRepository.findByBookEdition_Id(
                                edition.getId()
                        )
                )
                .flatMap(optional -> optional.stream())
                .toList();

        inventoryRepository.deleteAll(inventories);
        inventoryRepository.flush();
    }

    private void createEditions(
            Book book,
            List<BookEditionRequest> editionRequests,
            Map<Long, Publisher> publishers
    ) {
        for (BookEditionRequest request : editionRequests) {
            BookEdition edition = new BookEdition(
                    book,
                    request.format()
            );

            if (request.publisherId() != null) {
                edition.setPublisher(
                        publishers.get(request.publisherId())
                );
            }

            edition.setEditionName(
                    normalizeOptional(request.editionName())
            );
            edition.setIsbn10(normalizeIsbn10(request.isbn10()));
            edition.setIsbn13(normalizeOptional(request.isbn13()));
            edition.setPublicationDate(request.publicationDate());
            edition.setPageCount(request.pageCount());

            BookEdition savedEdition =
                    bookEditionRepository.save(edition);

            InventoryRequest inventoryRequest =
                    request.inventory();

            Inventory inventory = new Inventory(
                    savedEdition,
                    inventoryRequest.price(),
                    inventoryRequest.stockQuantity(),
                    inventoryRequest.availabilityStatus()
            );

            inventory.setActive(inventoryRequest.active());
            inventoryRepository.save(inventory);
        }
    }

    private BookResponse toResponse(Book book) {
        List<AuthorResponse> authors =
                bookAuthorRepository
                        .findAllByBook_Id(book.getId())
                        .stream()
                        .map(BookAuthor::getAuthor)
                        .sorted(Comparator.comparing(
                                Author::getName,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .map(this::toAuthorResponse)
                        .toList();

        List<CategoryResponse> categories =
                bookCategoryRepository
                        .findAllByBook_Id(book.getId())
                        .stream()
                        .map(BookCategory::getCategory)
                        .sorted(Comparator.comparing(
                                Category::getName,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .map(this::toCategoryResponse)
                        .toList();

        List<BookEditionResponse> editions =
                bookEditionRepository
                        .findAllByBook_Id(book.getId())
                        .stream()
                        .sorted(Comparator.comparing(
                                BookEdition::getId
                        ))
                        .map(this::toEditionResponse)
                        .toList();

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getSubtitle(),
                book.getDescription(),
                book.getCoverImageUrl(),
                book.getLanguageCode(),
                authors,
                categories,
                editions,
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }

    private BookEditionResponse toEditionResponse(
            BookEdition edition
    ) {
        PublisherResponse publisher = edition.getPublisher() == null
                ? null
                : toPublisherResponse(edition.getPublisher());

        Inventory inventory = inventoryRepository
                .findByBookEdition_Id(edition.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for book edition id: "
                                + edition.getId()
                ));

        return new BookEditionResponse(
                edition.getId(),
                publisher,
                edition.getFormat(),
                edition.getEditionName(),
                edition.getIsbn10(),
                edition.getIsbn13(),
                edition.getPublicationDate(),
                edition.getPageCount(),
                toInventoryResponse(inventory),
                edition.getCreatedAt(),
                edition.getUpdatedAt()
        );
    }

    private AuthorResponse toAuthorResponse(Author author) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getBiography(),
                author.getCreatedAt(),
                author.getUpdatedAt()
        );
    }

    private CategoryResponse toCategoryResponse(
            Category category
    ) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private PublisherResponse toPublisherResponse(
            Publisher publisher
    ) {
        return new PublisherResponse(
                publisher.getId(),
                publisher.getName(),
                publisher.getWebsiteUrl(),
                publisher.getCreatedAt(),
                publisher.getUpdatedAt()
        );
    }

    private InventoryResponse toInventoryResponse(
            Inventory inventory
    ) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getPrice(),
                inventory.getStockQuantity(),
                inventory.getAvailabilityStatus(),
                inventory.isActive(),
                inventory.getUpdatedAt()
        );
    }

    private String normalizeIsbn10(String value) {
        String normalized = normalizeOptional(value);

        return normalized == null
                ? null
                : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Sort createSort(BookSort requestedSort) {
        BookSort effectiveSort = requestedSort == null
                ? BookSort.TITLE_ASC
                : requestedSort;

        return switch (effectiveSort) {
            case TITLE_ASC -> Sort.by(
                    Sort.Order.asc("title").ignoreCase(),
                    Sort.Order.asc("id")
            );

            case TITLE_DESC -> Sort.by(
                    Sort.Order.desc("title").ignoreCase(),
                    Sort.Order.desc("id")
            );

            case NEWEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );

            case OLDEST -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id")
            );
        };
    }
}