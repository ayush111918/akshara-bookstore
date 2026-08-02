package com.akshara.api.book.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "books",
        indexes = {
                @Index(
                        name = "idx_books_title",
                        columnList = "title"
                ),
                @Index(
                        name = "idx_books_language",
                        columnList = "language_code"
                )
        }
)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Book() {
        // Required by JPA
    }

    public Book(String title) {
        this.title = title;
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

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
}