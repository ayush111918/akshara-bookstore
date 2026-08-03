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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "books",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_books_metadata_source_work",
                columnNames = {"metadata_source", "external_work_id"}
        ),
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

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "metadata_source", length = 30)
    private String metadataSource;

    @Column(name = "external_work_id", length = 255)
    private String externalWorkId;

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

    public boolean isFeatured() {
        return featured;
    }

    public String getMetadataSource() {
        return metadataSource;
    }

    public String getExternalWorkId() {
        return externalWorkId;
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

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public void setMetadataSource(String metadataSource) {
        this.metadataSource = metadataSource;
    }

    public void setExternalWorkId(String externalWorkId) {
        this.externalWorkId = externalWorkId;
    }
}
