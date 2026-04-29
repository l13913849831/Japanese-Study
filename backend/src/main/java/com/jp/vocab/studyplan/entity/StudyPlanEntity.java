package com.jp.vocab.studyplan.entity;

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
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "study_plan")
public class StudyPlanEntity extends AuditableEntity {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    private static final Set<String> EDITABLE_STATUSES = Set.of(STATUS_DRAFT, STATUS_PAUSED);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_set_id", nullable = false)
    private Long wordSetId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "daily_new_count", nullable = false)
    private Integer dailyNewCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "review_offsets", nullable = false, columnDefinition = "jsonb")
    private List<Integer> reviewOffsets;

    @Column(name = "anki_template_id")
    private Long ankiTemplateId;

    @Column(name = "md_template_id")
    private Long mdTemplateId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected StudyPlanEntity() {
    }

    public static StudyPlanEntity create(
            String name,
            Long userId,
            Long wordSetId,
            LocalDate startDate,
            Integer dailyNewCount,
            List<Integer> reviewOffsets,
            Long ankiTemplateId,
            Long mdTemplateId
    ) {
        StudyPlanEntity entity = new StudyPlanEntity();
        entity.name = name;
        entity.userId = userId;
        entity.wordSetId = wordSetId;
        entity.startDate = startDate;
        entity.dailyNewCount = dailyNewCount;
        entity.reviewOffsets = reviewOffsets;
        entity.ankiTemplateId = ankiTemplateId;
        entity.mdTemplateId = mdTemplateId;
        entity.status = STATUS_DRAFT;
        return entity;
    }

    public static StudyPlanEntity restore(
            String name,
            Long userId,
            Long wordSetId,
            LocalDate startDate,
            Integer dailyNewCount,
            List<Integer> reviewOffsets,
            Long ankiTemplateId,
            Long mdTemplateId,
            String status,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime updatedAt
    ) {
        StudyPlanEntity entity = create(
                name,
                userId,
                wordSetId,
                startDate,
                dailyNewCount,
                reviewOffsets,
                ankiTemplateId,
                mdTemplateId
        );
        entity.status = status;
        entity.restoreAuditTimestamps(createdAt, updatedAt);
        return entity;
    }

    public void update(
            String name,
            Long wordSetId,
            LocalDate startDate,
            Integer dailyNewCount,
            List<Integer> reviewOffsets,
            Long ankiTemplateId,
            Long mdTemplateId
    ) {
        this.name = name;
        this.wordSetId = wordSetId;
        this.startDate = startDate;
        this.dailyNewCount = dailyNewCount;
        this.reviewOffsets = reviewOffsets;
        this.ankiTemplateId = ankiTemplateId;
        this.mdTemplateId = mdTemplateId;
    }

    public boolean isEditable() {
        return EDITABLE_STATUSES.contains(status);
    }

    public boolean canActivate() {
        return STATUS_DRAFT.equals(status) || STATUS_PAUSED.equals(status);
    }

    public boolean canPause() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean canArchive() {
        return !STATUS_ARCHIVED.equals(status);
    }

    public void activate() {
        this.status = STATUS_ACTIVE;
    }

    public void pause() {
        this.status = STATUS_PAUSED;
    }

    public void archive() {
        this.status = STATUS_ARCHIVED;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getWordSetId() {
        return wordSetId;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public Integer getDailyNewCount() {
        return dailyNewCount;
    }

    public List<Integer> getReviewOffsets() {
        return reviewOffsets;
    }

    public Long getAnkiTemplateId() {
        return ankiTemplateId;
    }

    public Long getMdTemplateId() {
        return mdTemplateId;
    }

    public String getStatus() {
        return status;
    }
}
