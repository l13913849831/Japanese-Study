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
}
