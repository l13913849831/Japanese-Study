package com.jp.vocab.card.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "review_log")
public class ReviewLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_instance_id", nullable = false)
    private Long cardInstanceId;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    @Column(name = "rating", nullable = false)
    private String rating;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ReviewLogEntity() {
    }

    public static ReviewLogEntity create(
            Long cardInstanceId,
            OffsetDateTime reviewedAt,
            String rating,
            Long responseTimeMs,
            String note
    ) {
        ReviewLogEntity entity = new ReviewLogEntity();
        entity.cardInstanceId = cardInstanceId;
        entity.reviewedAt = reviewedAt;
        entity.rating = rating;
        entity.responseTimeMs = responseTimeMs;
        entity.note = note;
        entity.createdAt = OffsetDateTime.now();
        return entity;
    }

    public static ReviewLogEntity restore(
            Long cardInstanceId,
            OffsetDateTime reviewedAt,
            String rating,
            Long responseTimeMs,
            String note,
            OffsetDateTime createdAt
    ) {
        ReviewLogEntity entity = new ReviewLogEntity();
        entity.cardInstanceId = cardInstanceId;
        entity.reviewedAt = reviewedAt;
        entity.rating = rating;
        entity.responseTimeMs = responseTimeMs;
        entity.note = note;
        entity.createdAt = createdAt;
        return entity;
    }

    public Long getId() {
        return id;
    }

    public Long getCardInstanceId() {
        return cardInstanceId;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getRating() {
        return rating;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
