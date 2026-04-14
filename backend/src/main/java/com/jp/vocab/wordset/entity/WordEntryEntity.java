package com.jp.vocab.wordset.entity;

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
@Table(name = "word_entry")
public class WordEntryEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "word_set_id", nullable = false)
    private Long wordSetId;

    @Column(name = "expression", nullable = false)
    private String expression;

    @Column(name = "reading")
    private String reading;

    @Column(name = "meaning", nullable = false)
    private String meaning;

    @Column(name = "part_of_speech")
    private String partOfSpeech;

    @Column(name = "example_jp")
    private String exampleJp;

    @Column(name = "example_zh")
    private String exampleZh;

    @Column(name = "level")
    private String level;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", nullable = false, columnDefinition = "jsonb")
    private List<String> tags;

    @Column(name = "source_order", nullable = false)
    private Integer sourceOrder;

    protected WordEntryEntity() {
    }
}
