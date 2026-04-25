package com.jp.vocab.card.entity;

import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

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

    @Column(name = "status", nullable = false)
    private String status;

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
        entity.status = status;
        entity.weakFlag = false;
        entity.weakReviewCount = 0;
        return entity;
    }

    public void markDone() {
        this.status = "DONE";
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

    public String getStatus() {
        return status;
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
