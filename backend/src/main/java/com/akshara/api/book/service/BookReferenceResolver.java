package com.akshara.api.book.service;

import com.akshara.api.book.dto.BookEditionRequest;
import com.akshara.api.book.entity.Author;
import com.akshara.api.book.entity.Category;
import com.akshara.api.book.entity.Publisher;
import com.akshara.api.book.repository.AuthorRepository;
import com.akshara.api.book.repository.CategoryRepository;
import com.akshara.api.book.repository.PublisherRepository;
import com.akshara.api.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class BookReferenceResolver {

    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;

    BookReferenceResolver(
            AuthorRepository authorRepository,
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository
    ) {
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
    }

    List<Author> authors(Set<Long> ids) {
        List<Author> values = authorRepository.findAllById(ids);
        rejectMissing(ids, values.stream().map(Author::getId).collect(Collectors.toSet()), "Authors");
        return values;
    }

    List<Category> categories(Set<Long> ids) {
        List<Category> values = categoryRepository.findAllById(ids);
        rejectMissing(ids, values.stream().map(Category::getId).collect(Collectors.toSet()), "Categories");
        return values;
    }

    Map<Long, Publisher> publishers(List<BookEditionRequest> editions) {
        Set<Long> ids = editions.stream().map(BookEditionRequest::publisherId)
                .filter(id -> id != null).collect(Collectors.toSet());
        List<Publisher> values = publisherRepository.findAllById(ids);
        rejectMissing(ids, values.stream().map(Publisher::getId).collect(Collectors.toSet()), "Publishers");
        return values.stream().collect(Collectors.toMap(Publisher::getId, Function.identity()));
    }

    private void rejectMissing(Set<Long> requested, Set<Long> found, String resource) {
        List<Long> missing = requested.stream().filter(id -> !found.contains(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException(resource + " not found with ids: " + missing);
        }
    }
}
