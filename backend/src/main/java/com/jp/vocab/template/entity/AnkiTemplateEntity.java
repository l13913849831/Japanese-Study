package com.jp.vocab.template.entity;

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
import java.util.Map;

@Entity
@Table(name = "anki_template")
public class AnkiTemplateEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_mapping", nullable = false, columnDefinition = "jsonb")
    private Map<String, List<String>> fieldMapping;

    @Column(name = "front_template", nullable = false)
    private String frontTemplate;

    @Column(name = "back_template", nullable = false)
    private String backTemplate;

    @Column(name = "css_template")
    private String cssTemplate;

    protected AnkiTemplateEntity() {
    }

    public static AnkiTemplateEntity create(
            String name,
            String description,
            Map<String, List<String>> fieldMapping,
            String frontTemplate,
            String backTemplate,
            String cssTemplate
    ) {
        AnkiTemplateEntity entity = new AnkiTemplateEntity();
        entity.name = name;
        entity.description = description;
        entity.fieldMapping = fieldMapping;
        entity.frontTemplate = frontTemplate;
        entity.backTemplate = backTemplate;
        entity.cssTemplate = cssTemplate;
        return entity;
    }

    public void update(
            String name,
            String description,
            Map<String, List<String>> fieldMapping,
            String frontTemplate,
            String backTemplate,
            String cssTemplate
    ) {
        this.name = name;
        this.description = description;
        this.fieldMapping = fieldMapping;
        this.frontTemplate = frontTemplate;
        this.backTemplate = backTemplate;
        this.cssTemplate = cssTemplate;
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

    public Map<String, List<String>> getFieldMapping() {
        return fieldMapping;
    }

    public String getFrontTemplate() {
        return frontTemplate;
    }

    public String getBackTemplate() {
        return backTemplate;
    }

    public String getCssTemplate() {
        return cssTemplate;
    }
}
