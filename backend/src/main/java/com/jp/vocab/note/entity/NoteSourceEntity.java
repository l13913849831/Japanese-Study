package com.jp.vocab.note.entity;

import com.jp.vocab.shared.auth.ContentScope;
import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "note_source")
public class NoteSourceEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope", nullable = false, length = 32)
    private String scope;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", nullable = false, columnDefinition = "jsonb")
    private List<String> tags;

    protected NoteSourceEntity() {
    }

    public static NoteSourceEntity createUserOwned(
            String title,
            String content,
            List<String> tags,
            Long ownerUserId
    ) {
        NoteSourceEntity entity = new NoteSourceEntity();
        entity.scope = ContentScope.USER;
        entity.ownerUserId = ownerUserId;
        entity.title = title;
        entity.content = content;
        entity.tags = tags;
        return entity;
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

    public Long getId() {
        return id;
    }

    public String getScope() {
        return scope;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
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
}
