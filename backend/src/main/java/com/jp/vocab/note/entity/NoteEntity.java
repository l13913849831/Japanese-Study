package com.jp.vocab.note.entity;

import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "note")
public class NoteEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", nullable = false, columnDefinition = "jsonb")
    private List<String> tags;

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
            String title,
            String content,
            List<String> tags,
            Integer reviewCount,
            String masteryStatus,
            OffsetDateTime dueAt,
            OffsetDateTime lastReviewedAt,
            String fsrsCardJson
    ) {
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.reviewCount = reviewCount;
        this.masteryStatus = masteryStatus;
        this.dueAt = dueAt;
        this.lastReviewedAt = lastReviewedAt;
        this.fsrsCardJson = fsrsCardJson;
    }

    public static NoteEntity create(
            String title,
            String content,
            List<String> tags,
            OffsetDateTime dueAt,
            String fsrsCardJson
    ) {
        return new NoteEntity(
                title,
                content,
                tags,
                0,
                "UNSTARTED",
                dueAt,
                null,
                fsrsCardJson
        );
    }

    public void update(
            String title,
            String content,
            List<String> tags
    ) {
        this.title = title;
        this.content = content;
        this.tags = tags;
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

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public List<String> getTags() {
        return tags;
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
}
