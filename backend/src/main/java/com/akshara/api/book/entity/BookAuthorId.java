package com.akshara.api.book.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BookAuthorId implements Serializable {

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "author_id")
    private Long authorId;

    protected BookAuthorId() {
        // Required by JPA
    }

    public BookAuthorId(Long bookId, Long authorId) {
        this.bookId = bookId;
        this.authorId = authorId;
    }

    public Long getBookId() {
        return bookId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof BookAuthorId other)) {
            return false;
        }

        return Objects.equals(bookId, other.bookId)
                && Objects.equals(authorId, other.authorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookId, authorId);
    }
}