package com.jp.vocab.template.entity;

import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "md_template")
public class MarkdownTemplateEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "scope", nullable = false, length = 32)
    private String scope;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "template_content", nullable = false)
    private String templateContent;

    protected MarkdownTemplateEntity() {
    }

    public static MarkdownTemplateEntity create(
            String name,
            String description,
            String scope,
            Long ownerUserId,
            String templateContent
    ) {
        MarkdownTemplateEntity entity = new MarkdownTemplateEntity();
        entity.name = name;
        entity.description = description;
        entity.scope = scope;
        entity.ownerUserId = ownerUserId;
        entity.templateContent = templateContent;
        return entity;
    }

    public static MarkdownTemplateEntity restore(
            String name,
            String description,
            String scope,
            Long ownerUserId,
            String templateContent,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime updatedAt
    ) {
        MarkdownTemplateEntity entity = create(name, description, scope, ownerUserId, templateContent);
        entity.restoreAuditTimestamps(createdAt, updatedAt);
        return entity;
    }

    public void update(
            String name,
            String description,
            String templateContent
    ) {
        this.name = name;
        this.description = description;
        this.templateContent = templateContent;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getScope() {
        return scope;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public String getTemplateContent() {
        return templateContent;
    }
}
