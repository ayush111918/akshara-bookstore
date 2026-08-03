package com.akshara.api.reading.entity;

import com.akshara.api.user.entity.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "reading_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reading_entries_user_source",
                columnNames = {"user_id", "source_type", "source_reference_id"}
        ),
        indexes = {
                @Index(name = "idx_reading_entries_user_status", columnList = "user_id, status"),
                @Index(name = "idx_reading_entries_user_updated", columnList = "user_id, updated_at")
        }
)
public class ReadingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ReadingSourceType sourceType;

    @Column(name = "source_reference_id", nullable = false)
    private Long sourceReferenceId;

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "title_snapshot", nullable = false, length = 255)
    private String titleSnapshot;

    @Column(name = "author_snapshot", length = 180)
    private String authorSnapshot;

    @Column(name = "cover_image_url_snapshot", length = 1000)
    private String coverImageUrlSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingStatus status = ReadingStatus.NOT_STARTED;

    @Column(name = "current_page", nullable = false)
    private Integer currentPage = 0;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "completed_on")
    private LocalDate completedOn;

    @Column(name = "last_activity_on")
    private LocalDate lastActivityOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReadingEntry() {
    }

    public ReadingEntry(
            AppUser user,
            ReadingSourceType sourceType,
            Long sourceReferenceId,
            Long bookId,
            String titleSnapshot,
            String authorSnapshot,
            String coverImageUrlSnapshot,
            Integer totalPages
    ) {
        this.user = user;
        this.sourceType = sourceType;
        this.sourceReferenceId = sourceReferenceId;
        this.bookId = bookId;
        this.titleSnapshot = titleSnapshot;
        this.authorSnapshot = authorSnapshot;
        this.coverImageUrlSnapshot = coverImageUrlSnapshot;
        this.totalPages = totalPages;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = ReadingStatus.NOT_STARTED;
        if (currentPage == null) currentPage = 0;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public void updateProgress(
            ReadingStatus nextStatus,
            int nextPage,
            Integer nextTotalPages,
            LocalDate activityDate
    ) {
        status = nextStatus;
        currentPage = nextPage;
        totalPages = nextTotalPages;
        lastActivityOn = activityDate;

        if (nextStatus != ReadingStatus.NOT_STARTED && startedOn == null) {
            startedOn = activityDate;
        }
        if (nextStatus == ReadingStatus.COMPLETED) {
            completedOn = activityDate;
            if (nextTotalPages != null) currentPage = nextTotalPages;
        } else {
            completedOn = null;
        }
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public ReadingSourceType getSourceType() { return sourceType; }
    public Long getSourceReferenceId() { return sourceReferenceId; }
    public Long getBookId() { return bookId; }
    public String getTitleSnapshot() { return titleSnapshot; }
    public String getAuthorSnapshot() { return authorSnapshot; }
    public String getCoverImageUrlSnapshot() { return coverImageUrlSnapshot; }
    public ReadingStatus getStatus() { return status; }
    public Integer getCurrentPage() { return currentPage; }
    public Integer getTotalPages() { return totalPages; }
    public LocalDate getStartedOn() { return startedOn; }
    public LocalDate getCompletedOn() { return completedOn; }
    public LocalDate getLastActivityOn() { return lastActivityOn; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
