package com.jp.vocab.exportjob.entity;

import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "export_job")
public class ExportJobEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "export_type", nullable = false, length = 32)
    private String exportType;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected ExportJobEntity() {
    }

    public static ExportJobEntity create(
            Long planId,
            String exportType,
            LocalDate targetDate,
            String fileName,
            String filePath,
            String status
    ) {
        ExportJobEntity entity = new ExportJobEntity();
        entity.planId = planId;
        entity.exportType = exportType;
        entity.targetDate = targetDate;
        entity.fileName = fileName;
        entity.filePath = filePath;
        entity.status = status;
        return entity;
    }

    public void updateResult(String fileName, String filePath, String status) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getExportType() {
        return exportType;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getStatus() {
        return status;
    }
}
