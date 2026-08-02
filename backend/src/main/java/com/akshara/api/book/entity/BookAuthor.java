package com.akshara.api.book.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "book_authors",
        indexes = {
                @Index(
                        name = "idx_book_authors_author",
                        columnList = "author_id"
                )
        }
)
public class BookAuthor {

    @EmbeddedId
    private BookAuthorId id = new BookAuthorId();

    @MapsId("bookId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @MapsId("authorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BookAuthor() {
        // Required by JPA
    }

    public BookAuthor(Book book, Author author) {
        this.book = book;
        this.author = author;
    }

    @PrePersist
    void beforeInsert() {
        this.createdAt = Instant.now();
    }

    public BookAuthorId getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public Author getAuthor() {
        return author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}