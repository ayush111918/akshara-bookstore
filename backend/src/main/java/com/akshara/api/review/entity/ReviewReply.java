package com.akshara.api.review.entity;

import com.akshara.api.user.entity.AppUser;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "review_replies", indexes = {
        @Index(name = "idx_review_replies_review_created", columnList = "review_id, created_at"),
        @Index(name = "idx_review_replies_user", columnList = "user_id")
})
public class ReviewReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReviewReply() { }

    public ReviewReply(Review review, AppUser user, String content) {
        this.review = review;
        this.user = user;
        this.content = content;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Review getReview() { return review; }
    public AppUser getUser() { return user; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void update(String content) { this.content = content; }
}
