package com.jp.vocab.card.entity;

import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "card_instance")
public class CardInstanceEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "word_entry_id", nullable = false)
    private Long wordEntryId;

    @Column(name = "card_type", nullable = false)
    private String cardType;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "stage_no", nullable = false)
    private Integer stageNo;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Column(name = "status", nullable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fsrs_card_json", columnDefinition = "jsonb")
    private String fsrsCardJson;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

    @Column(name = "weak_flag", nullable = false)
    private boolean weakFlag;

    @Column(name = "weak_marked_at")
    private OffsetDateTime weakMarkedAt;

    @Column(name = "weak_review_count", nullable = false)
    private Integer weakReviewCount;

    @Column(name = "last_review_rating", length = 16)
    private String lastReviewRating;

    protected CardInstanceEntity() {
    }

    public static CardInstanceEntity create(
            Long planId,
            Long wordEntryId,
            String cardType,
            Integer sequenceNo,
            Integer stageNo,
            LocalDate dueDate,
            String status
    ) {
        CardInstanceEntity entity = new CardInstanceEntity();
        entity.planId = planId;
        entity.wordEntryId = wordEntryId;
        entity.cardType = cardType;
        entity.sequenceNo = sequenceNo;
        entity.stageNo = stageNo;
        entity.dueDate = dueDate;
        entity.dueAt = dueDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        entity.status = status;
        entity.reviewCount = 0;
        entity.weakFlag = false;
        entity.weakReviewCount = 0;
        return entity;
    }

    public static CardInstanceEntity createFsrsCard(
            Long planId,
            Long wordEntryId,
            String cardType,
            Integer sequenceNo,
            Integer stageNo,
            OffsetDateTime dueAt,
            String status,
            String fsrsCardJson,
            Integer reviewCount,
            OffsetDateTime lastReviewedAt
    ) {
        CardInstanceEntity entity = new CardInstanceEntity();
        entity.planId = planId;
        entity.wordEntryId = wordEntryId;
        entity.cardType = cardType;
        entity.sequenceNo = sequenceNo;
        entity.stageNo = stageNo;
        entity.dueAt = dueAt;
        entity.dueDate = dueAt.toLocalDate();
        entity.status = status;
        entity.fsrsCardJson = fsrsCardJson;
        entity.reviewCount = reviewCount;
        entity.lastReviewedAt = lastReviewedAt;
        entity.weakFlag = false;
        entity.weakReviewCount = 0;
        return entity;
    }

    public void markDone(OffsetDateTime reviewedAt) {
        this.status = "DONE";
        this.lastReviewedAt = reviewedAt;
    }

    public void applyReviewResult(String rating, Integer sessionAgainCount, OffsetDateTime reviewedAt) {
        this.lastReviewRating = rating;
        if ("GOOD".equals(rating) || "EASY".equals(rating)) {
            clearWeak();
        } else if ("AGAIN".equals(rating) && sessionAgainCount != null && sessionAgainCount >= 2) {
            markWeak(reviewedAt);
        } else if (weakFlag) {
            weakReviewCount = weakReviewCount + 1;
        }
    }

    public CardInstanceEntity createNextReviewCard(
            String nextFsrsCardJson,
            Integer nextReviewCount,
            OffsetDateTime nextDueAt
    ) {
        return createFsrsCard(
                planId,
                wordEntryId,
                "REVIEW",
                sequenceNo,
                stageNo + 1,
                nextDueAt,
                "PENDING",
                nextFsrsCardJson,
                nextReviewCount,
                lastReviewedAt
        );
    }

    public void clearWeak() {
        this.weakFlag = false;
        this.weakMarkedAt = null;
        this.weakReviewCount = 0;
    }

    private void markWeak(OffsetDateTime reviewedAt) {
        if (!weakFlag) {
            this.weakFlag = true;
            this.weakMarkedAt = reviewedAt;
            this.weakReviewCount = 0;
            return;
        }
        this.weakReviewCount = weakReviewCount + 1;
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public Long getWordEntryId() {
        return wordEntryId;
    }

    public String getCardType() {
        return cardType;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public Integer getStageNo() {
        return stageNo;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }

    public String getStatus() {
        return status;
    }

    public String getFsrsCardJson() {
        return fsrsCardJson;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public OffsetDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public boolean isWeakFlag() {
        return weakFlag;
    }

    public OffsetDateTime getWeakMarkedAt() {
        return weakMarkedAt;
    }

    public Integer getWeakReviewCount() {
        return weakReviewCount;
    }

    public String getLastReviewRating() {
        return lastReviewRating;
    }
}
