package com.akshara.api.reading.entity;

import com.akshara.api.user.entity.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "reading_activities",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reading_activities_entry_date",
                columnNames = {"reading_entry_id", "activity_date"}
        ),
        indexes = @Index(
                name = "idx_reading_activities_user_date",
                columnList = "user_id, activity_date"
        )
)
public class ReadingActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_entry_id", nullable = false)
    private ReadingEntry readingEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "pages_read", nullable = false)
    private Integer pagesRead;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReadingActivity() {
    }

    public ReadingActivity(
            ReadingEntry readingEntry,
            AppUser user,
            LocalDate activityDate,
            int pagesRead
    ) {
        this.readingEntry = readingEntry;
        this.user = user;
        this.activityDate = activityDate;
        this.pagesRead = Math.max(0, pagesRead);
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public void addPages(int pages) {
        pagesRead += Math.max(0, pages);
    }

    public Long getId() { return id; }
    public ReadingEntry getReadingEntry() { return readingEntry; }
    public AppUser getUser() { return user; }
    public LocalDate getActivityDate() { return activityDate; }
    public Integer getPagesRead() { return pagesRead; }
    public Instant getUpdatedAt() { return updatedAt; }
}
