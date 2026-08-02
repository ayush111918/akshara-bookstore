package com.akshara.api.book.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "book_editions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_book_editions_isbn10",
                        columnNames = "isbn10"
                ),
                @UniqueConstraint(
                        name = "uk_book_editions_isbn13",
                        columnNames = "isbn13"
                )
        },
        indexes = {
                @Index(
                        name = "idx_book_editions_book",
                        columnList = "book_id"
                ),
                @Index(
                        name = "idx_book_editions_publisher",
                        columnList = "publisher_id"
                ),
                @Index(
                        name = "idx_book_editions_format",
                        columnList = "format"
                )
        }
)
public class BookEdition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookFormat format;

    @Column(name = "edition_name", length = 100)
    private String editionName;

    @Column(length = 10)
    private String isbn10;

    @Column(length = 13)
    private String isbn13;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BookEdition() {
        // Required by JPA
    }

    public BookEdition(Book book, BookFormat format) {
        this.book = book;
        this.format = format;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public BookFormat getFormat() {
        return format;
    }

    public String getEditionName() {
        return editionName;
    }

    public String getIsbn10() {
        return isbn10;
    }

    public String getIsbn13() {
        return isbn13;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public void setFormat(BookFormat format) {
        this.format = format;
    }

    public void setEditionName(String editionName) {
        this.editionName = editionName;
    }

    public void setIsbn10(String isbn10) {
        this.isbn10 = isbn10;
    }

    public void setIsbn13(String isbn13) {
        this.isbn13 = isbn13;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }
}