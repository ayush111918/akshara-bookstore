package com.akshara.api.reading.entity;

import com.akshara.api.user.entity.AppUser;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "reading_goals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reading_goals_user_year",
                columnNames = {"user_id", "goal_year"}
        )
)
public class ReadingGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "goal_year", nullable = false)
    private Integer year;

    @Column(name = "target_books", nullable = false)
    private Integer targetBooks;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReadingGoal() {
    }

    public ReadingGoal(AppUser user, int year, int targetBooks) {
        this.user = user;
        this.year = year;
        this.targetBooks = targetBooks;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public void setTargetBooks(int targetBooks) {
        this.targetBooks = targetBooks;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Integer getYear() { return year; }
    public Integer getTargetBooks() { return targetBooks; }
    public Instant getUpdatedAt() { return updatedAt; }
}
