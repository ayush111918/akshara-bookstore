package com.akshara.api.book.service;

import com.akshara.api.book.dto.BookPageResponse;
import com.akshara.api.book.dto.BookRequest;
import com.akshara.api.book.dto.BookResponse;
import com.akshara.api.book.dto.BookSearchCriteria;
import com.akshara.api.book.dto.BookSort;
import com.akshara.api.book.dto.InventoryRequest;
import com.akshara.api.book.dto.InventoryResponse;
import com.akshara.api.book.entity.Author;
import com.akshara.api.book.entity.Book;
import com.akshara.api.book.entity.Category;
import com.akshara.api.book.entity.Publisher;
import com.akshara.api.book.repository.BookRepository;
import com.akshara.api.book.specification.BookSpecifications;
import com.akshara.api.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository books;
    private final BookReferenceResolver references;
    private final BookRequestValidator validator;
    private final BookRelationshipManager relationships;
    private final BookEditionManager editions;
    private final BookInventoryManager inventory;
    private final BookResponseMapper mapper;
    private final BookDeletionManager deletion;

    public BookService(
            BookRepository books,
            BookReferenceResolver references,
            BookRequestValidator validator,
            BookRelationshipManager relationships,
            BookEditionManager editions,
            BookInventoryManager inventory,
            BookResponseMapper mapper,
            BookDeletionManager deletion
    ) {
        this.books = books;
        this.references = references;
        this.validator = validator;
        this.relationships = relationships;
        this.editions = editions;
        this.inventory = inventory;
        this.mapper = mapper;
        this.deletion = deletion;
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        validator.validate(request.editions(), null);
        List<Author> authors = references.authors(request.authorIds());
        List<Category> categories = references.categories(request.categoryIds());
        Map<Long, Publisher> publishers = references.publishers(request.editions());

        Book book = new Book(request.title().trim());
        applyFields(book, request);
        book = books.save(book);
        relationships.create(book, authors, categories);
        editions.create(book, request.editions(), publishers, null);
        return mapper.response(book);
    }

    @Transactional
    public BookResponse createImported(
            BookRequest request,
            String metadataSource,
            String externalWorkId,
            String externalEditionId
    ) {
        validator.validate(request.editions(), null);
        List<Author> authors = references.authors(request.authorIds());
        List<Category> categories = references.categories(request.categoryIds());
        Map<Long, Publisher> publishers = references.publishers(request.editions());

        String source = validator.normalizeOptional(metadataSource);
        String workId = validator.normalizeOptional(externalWorkId);
        Book book = source == null || workId == null ? null
                : books.findByMetadataSourceAndExternalWorkId(source, workId).orElse(null);
        if (book == null) {
            book = new Book(request.title().trim());
            book.setMetadataSource(source);
            book.setExternalWorkId(workId);
            applyFields(book, request);
            book = books.save(book);
            relationships.create(book, authors, categories);
        } else {
            applyFields(book, request);
            books.save(book);
            relationships.replace(book, authors, categories);
        }
        editions.create(
                book, request.editions(), publishers,
                validator.normalizeOptional(externalEditionId)
        );
        return mapper.response(book);
    }

    public List<BookResponse> getAll() {
        return books.findAll(Sort.by(Sort.Direction.ASC, "title").and(
                        Sort.by(Sort.Direction.ASC, "id")))
                .stream().map(mapper::response).toList();
    }

    public BookPageResponse search(
            BookSearchCriteria criteria,
            int page,
            int size,
            BookSort sort
    ) {
        Pageable pageable = PageRequest.of(page, size, createSort(sort));
        Page<Book> result = books.findAll(BookSpecifications.withCriteria(criteria), pageable);
        List<BookResponse> content = result.getContent().stream().map(mapper::response).toList();
        return new BookPageResponse(
                content, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isFirst(), result.isLast()
        );
    }

    public BookResponse getById(Long id) {
        return mapper.response(find(id));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = find(id);
        validator.validate(request.editions(), id);
        List<Author> authors = references.authors(request.authorIds());
        List<Category> categories = references.categories(request.categoryIds());
        Map<Long, Publisher> publishers = references.publishers(request.editions());

        applyFields(book, request);
        books.save(book);
        relationships.replace(book, authors, categories);
        editions.replace(book, request.editions(), publishers);
        return mapper.response(book);
    }

    @Transactional
    public BookResponse updateFeatured(Long id, boolean featured) {
        Book book = find(id);
        book.setFeatured(featured);
        books.save(book);
        return mapper.response(book);
    }

    @Transactional
    public InventoryResponse updateInventory(Long editionId, InventoryRequest request) {
        return inventory.update(editionId, request);
    }

    @Transactional
    public InventoryResponse restockInventory(Long editionId, int quantity) {
        return inventory.restock(editionId, quantity);
    }

    @Transactional
    public void delete(Long id) {
        deletion.delete(find(id));
    }

    private Book find(Long id) {
        return books.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Book not found with id: " + id));
    }

    private void applyFields(Book book, BookRequest request) {
        book.setTitle(request.title().trim());
        book.setSubtitle(validator.normalizeOptional(request.subtitle()));
        book.setDescription(validator.normalizeOptional(request.description()));
        book.setCoverImageUrl(validator.normalizeOptional(request.coverImageUrl()));
        book.setLanguageCode(validator.normalizeOptional(request.languageCode()));
        book.setFeatured(request.featured());
    }

    private Sort createSort(BookSort requested) {
        BookSort effective = requested == null ? BookSort.TITLE_ASC : requested;
        return switch (effective) {
            case TITLE_ASC -> Sort.by(
                    Sort.Order.asc("title").ignoreCase(), Sort.Order.asc("id"));
            case TITLE_DESC -> Sort.by(
                    Sort.Order.desc("title").ignoreCase(), Sort.Order.desc("id"));
            case NEWEST -> Sort.by(
                    Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case OLDEST -> Sort.by(
                    Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
        };
    }
}
