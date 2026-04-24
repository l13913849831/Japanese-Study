package com.jp.vocab.note.entity;

import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "note_review_log")
public class NoteReviewLogEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    @Column(name = "rating", nullable = false)
    private String rating;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "note_text")
    private String noteText;

    @Column(name = "fsrs_review_log_json", nullable = false)
    private String fsrsReviewLogJson;

    protected NoteReviewLogEntity() {
    }

    private NoteReviewLogEntity(
            Long noteId,
            OffsetDateTime reviewedAt,
            String rating,
            Long responseTimeMs,
            String noteText,
            String fsrsReviewLogJson
    ) {
        this.noteId = noteId;
        this.reviewedAt = reviewedAt;
        this.rating = rating;
        this.responseTimeMs = responseTimeMs;
        this.noteText = noteText;
        this.fsrsReviewLogJson = fsrsReviewLogJson;
    }

    public static NoteReviewLogEntity create(
            Long noteId,
            OffsetDateTime reviewedAt,
            String rating,
            Long responseTimeMs,
            String noteText,
            String fsrsReviewLogJson
    ) {
        return new NoteReviewLogEntity(
                noteId,
                reviewedAt,
                rating,
                responseTimeMs,
                noteText,
                fsrsReviewLogJson
        );
    }

    public Long getId() {
        return id;
    }

    public Long getNoteId() {
        return noteId;
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

    public String getNoteText() {
        return noteText;
    }

    public String getFsrsReviewLogJson() {
        return fsrsReviewLogJson;
    }
}
