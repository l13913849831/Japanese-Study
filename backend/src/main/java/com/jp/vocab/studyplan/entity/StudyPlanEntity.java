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

@Entity
@Table(name = "study_plan")
public class StudyPlanEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getWordSetId() {
        return wordSetId;
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
