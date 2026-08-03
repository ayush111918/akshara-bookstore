package com.akshara.api.library.entity;

import com.akshara.api.book.entity.BookFormat;
import com.akshara.api.user.entity.AppUser;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "personal_books", indexes = {
        @Index(name = "idx_personal_books_user_created", columnList = "user_id, created_at")
})
public class PersonalBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 180)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookFormat format;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true, length = 100)
    private String storedFilename;

    @Column(name = "media_type", nullable = false, length = 100)
    private String mediaType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PersonalBook() {
    }

    public PersonalBook(AppUser user, String title, String author, BookFormat format,
                        String originalFilename, String storedFilename, String mediaType, Long fileSize) {
        this.user = user;
        this.title = title;
        this.author = author;
        this.format = format;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.mediaType = mediaType;
        this.fileSize = fileSize;
    }

    @PrePersist
    void beforeInsert() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public BookFormat getFormat() { return format; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public String getMediaType() { return mediaType; }
    public Long getFileSize() { return fileSize; }
    public Instant getCreatedAt() { return createdAt; }
}
