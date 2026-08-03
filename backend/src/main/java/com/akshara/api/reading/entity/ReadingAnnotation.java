package com.akshara.api.reading.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reading_annotations", indexes = {
        @Index(name = "idx_reading_annotations_entry_created", columnList = "reading_entry_id, created_at")
})
public class ReadingAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_entry_id", nullable = false)
    private ReadingEntry readingEntry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingAnnotationType type;

    @Column(nullable = false, length = 3000)
    private String content;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReadingAnnotation() {
    }

    public ReadingAnnotation(
            ReadingEntry readingEntry,
            ReadingAnnotationType type,
            String content,
            Integer pageNumber
    ) {
        this.readingEntry = readingEntry;
        this.type = type;
        this.content = content;
        this.pageNumber = pageNumber;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public void update(ReadingAnnotationType type, String content, Integer pageNumber) {
        this.type = type;
        this.content = content;
        this.pageNumber = pageNumber;
    }

    public Long getId() { return id; }
    public ReadingEntry getReadingEntry() { return readingEntry; }
    public ReadingAnnotationType getType() { return type; }
    public String getContent() { return content; }
    public Integer getPageNumber() { return pageNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
