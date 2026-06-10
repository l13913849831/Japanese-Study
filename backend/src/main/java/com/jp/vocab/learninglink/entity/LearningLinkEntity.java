package com.jp.vocab.learninglink.entity;

import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_link")
public class LearningLinkEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_entry_id", nullable = false)
    private Long wordEntryId;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    protected LearningLinkEntity() {
    }

    private LearningLinkEntity(Long userId, Long wordEntryId, Long noteId, String source) {
        this.userId = userId;
        this.wordEntryId = wordEntryId;
        this.noteId = noteId;
        this.source = source;
    }

    public static LearningLinkEntity create(Long userId, Long wordEntryId, Long noteId, String source) {
        return new LearningLinkEntity(userId, wordEntryId, noteId, source);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getWordEntryId() {
        return wordEntryId;
    }

    public Long getNoteId() {
        return noteId;
    }

    public String getSource() {
        return source;
    }
}
