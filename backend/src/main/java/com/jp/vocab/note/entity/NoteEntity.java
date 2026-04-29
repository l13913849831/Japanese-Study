package com.jp.vocab.note.entity;

import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;

import java.time.OffsetDateTime;

@Entity
@Table(name = "note")
public class NoteEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_source_id", nullable = false)
    private NoteSourceEntity noteSource;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "mastery_status", nullable = false)
    private String masteryStatus;

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

    @Column(name = "fsrs_card_json", nullable = false)
    private String fsrsCardJson;

    @Column(name = "weak_flag", nullable = false)
    private boolean weakFlag;

    @Column(name = "weak_marked_at")
    private OffsetDateTime weakMarkedAt;

    @Column(name = "last_review_rating", length = 16)
    private String lastReviewRating;

    protected NoteEntity() {
    }

    private NoteEntity(
            NoteSourceEntity noteSource,
            Long userId,
            Integer reviewCount,
            String masteryStatus,
            OffsetDateTime dueAt,
            OffsetDateTime lastReviewedAt,
            String fsrsCardJson
    ) {
        this.noteSource = noteSource;
        this.userId = userId;
        this.reviewCount = reviewCount;
        this.masteryStatus = masteryStatus;
        this.dueAt = dueAt;
        this.lastReviewedAt = lastReviewedAt;
        this.fsrsCardJson = fsrsCardJson;
    }

    public static NoteEntity create(
            NoteSourceEntity noteSource,
            Long userId,
            OffsetDateTime dueAt,
            String fsrsCardJson
    ) {
        return new NoteEntity(
                noteSource,
                userId,
                0,
                "UNSTARTED",
                dueAt,
                null,
                fsrsCardJson
        );
    }

    public static NoteEntity restore(
            NoteSourceEntity noteSource,
            Long userId,
            Integer reviewCount,
            String masteryStatus,
            OffsetDateTime dueAt,
            OffsetDateTime lastReviewedAt,
            String fsrsCardJson,
            boolean weakFlag,
            OffsetDateTime weakMarkedAt,
            String lastReviewRating,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        NoteEntity entity = new NoteEntity(
                noteSource,
                userId,
                reviewCount,
                masteryStatus,
                dueAt,
                lastReviewedAt,
                fsrsCardJson
        );
        entity.weakFlag = weakFlag;
        entity.weakMarkedAt = weakMarkedAt;
        entity.lastReviewRating = lastReviewRating;
        entity.restoreAuditTimestamps(createdAt, updatedAt);
        return entity;
    }

    public void applyReview(
            Integer reviewCount,
            String masteryStatus,
            OffsetDateTime dueAt,
            OffsetDateTime lastReviewedAt,
            String fsrsCardJson,
            String rating,
            Integer sessionAgainCount
    ) {
        this.reviewCount = reviewCount;
        this.masteryStatus = masteryStatus;
        this.dueAt = dueAt;
        this.lastReviewedAt = lastReviewedAt;
        this.fsrsCardJson = fsrsCardJson;
        this.lastReviewRating = rating;
        if ("GOOD".equals(rating) || "EASY".equals(rating)) {
            clearWeak();
        } else if ("AGAIN".equals(rating) && sessionAgainCount != null && sessionAgainCount >= 2) {
            markWeak(lastReviewedAt);
        }
    }

    public void clearWeak() {
        this.weakFlag = false;
        this.weakMarkedAt = null;
    }

    private void markWeak(OffsetDateTime reviewedAt) {
        if (!weakFlag) {
            this.weakFlag = true;
            this.weakMarkedAt = reviewedAt;
        }
    }

    public Long getId() {
        return id;
    }

    public NoteSourceEntity getNoteSource() {
        return noteSource;
    }

    public Long getNoteSourceId() {
        return noteSource.getId();
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return noteSource.getTitle();
    }

    public String getContent() {
        return noteSource.getContent();
    }

    public java.util.List<String> getTags() {
        return noteSource.getTags();
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public String getMasteryStatus() {
        return masteryStatus;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }

    public OffsetDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public String getFsrsCardJson() {
        return fsrsCardJson;
    }

    public boolean isWeakFlag() {
        return weakFlag;
    }

    public OffsetDateTime getWeakMarkedAt() {
        return weakMarkedAt;
    }

    public String getLastReviewRating() {
        return lastReviewRating;
    }

    public OffsetDateTime getDisplayCreatedAt() {
        return noteSource.getCreatedAt().isBefore(getCreatedAt()) ? noteSource.getCreatedAt() : getCreatedAt();
    }

    public OffsetDateTime getDisplayUpdatedAt() {
        return noteSource.getUpdatedAt().isAfter(getUpdatedAt()) ? noteSource.getUpdatedAt() : getUpdatedAt();
    }
}
