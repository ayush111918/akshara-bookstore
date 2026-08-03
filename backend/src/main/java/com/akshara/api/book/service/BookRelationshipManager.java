package com.akshara.api.book.service;

import com.akshara.api.book.entity.Author;
import com.akshara.api.book.entity.Book;
import com.akshara.api.book.entity.BookAuthor;
import com.akshara.api.book.entity.BookCategory;
import com.akshara.api.book.entity.Category;
import com.akshara.api.book.repository.BookAuthorRepository;
import com.akshara.api.book.repository.BookCategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class BookRelationshipManager {

    private final BookAuthorRepository authorLinks;
    private final BookCategoryRepository categoryLinks;

    BookRelationshipManager(
            BookAuthorRepository authorLinks,
            BookCategoryRepository categoryLinks
    ) {
        this.authorLinks = authorLinks;
        this.categoryLinks = categoryLinks;
    }

    void create(Book book, List<Author> authors, List<Category> categories) {
        authorLinks.saveAll(authors.stream().map(author -> new BookAuthor(book, author)).toList());
        categoryLinks.saveAll(categories.stream().map(category -> new BookCategory(book, category)).toList());
    }

    void replace(Book book, List<Author> authors, List<Category> categories) {
        replaceAuthors(book, authors);
        replaceCategories(book, categories);
    }

    void deleteForBook(Long bookId) {
        authorLinks.deleteAll(authorLinks.findAllByBook_Id(bookId));
        authorLinks.flush();
        categoryLinks.deleteAll(categoryLinks.findAllByBook_Id(bookId));
        categoryLinks.flush();
    }

    private void replaceAuthors(Book book, List<Author> requested) {
        List<BookAuthor> existing = authorLinks.findAllByBook_Id(book.getId());
        Set<Long> requestedIds = requested.stream().map(Author::getId).collect(Collectors.toSet());
        Set<Long> existingIds = existing.stream().map(link -> link.getAuthor().getId())
                .collect(Collectors.toSet());
        authorLinks.deleteAll(existing.stream()
                .filter(link -> !requestedIds.contains(link.getAuthor().getId())).toList());
        authorLinks.saveAll(requested.stream().filter(author -> !existingIds.contains(author.getId()))
                .map(author -> new BookAuthor(book, author)).toList());
    }

    private void replaceCategories(Book book, List<Category> requested) {
        List<BookCategory> existing = categoryLinks.findAllByBook_Id(book.getId());
        Set<Long> requestedIds = requested.stream().map(Category::getId).collect(Collectors.toSet());
        Set<Long> existingIds = existing.stream().map(link -> link.getCategory().getId())
                .collect(Collectors.toSet());
        categoryLinks.deleteAll(existing.stream()
                .filter(link -> !requestedIds.contains(link.getCategory().getId())).toList());
        categoryLinks.saveAll(requested.stream().filter(category -> !existingIds.contains(category.getId()))
                .map(category -> new BookCategory(book, category)).toList());
    }
}
