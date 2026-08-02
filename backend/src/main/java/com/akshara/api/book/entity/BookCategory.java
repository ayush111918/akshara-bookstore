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
        name = "book_categories",
        indexes = {
                @Index(
                        name = "idx_book_categories_category",
                        columnList = "category_id"
                )
        }
)
public class BookCategory {

    @EmbeddedId
    private BookCategoryId id = new BookCategoryId();

    @MapsId("bookId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @MapsId("categoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BookCategory() {
        // Required by JPA
    }

    public BookCategory(Book book, Category category) {
        this.book = book;
        this.category = category;
    }

    @PrePersist
    void beforeInsert() {
        this.createdAt = Instant.now();
    }

    public BookCategoryId getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public Category getCategory() {
        return category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}