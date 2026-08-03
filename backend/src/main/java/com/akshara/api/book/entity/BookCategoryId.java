package com.akshara.api.book.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BookCategoryId implements Serializable {

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "category_id")
    private Long categoryId;

    protected BookCategoryId() {
        // Required by JPA
    }

    public BookCategoryId(Long bookId, Long categoryId) {
        this.bookId = bookId;
        this.categoryId = categoryId;
    }

    public Long getBookId() {
        return bookId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof BookCategoryId other)) {
            return false;
        }

        return Objects.equals(bookId, other.bookId)
                && Objects.equals(categoryId, other.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookId, categoryId);
    }
}