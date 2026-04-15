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

    private WordEntryEntity(
            Long wordSetId,
            String expression,
            String reading,
            String meaning,
            String partOfSpeech,
            String exampleJp,
            String exampleZh,
            String level,
            List<String> tags,
            Integer sourceOrder
    ) {
        this.wordSetId = wordSetId;
        this.expression = expression;
        this.reading = reading;
        this.meaning = meaning;
        this.partOfSpeech = partOfSpeech;
        this.exampleJp = exampleJp;
        this.exampleZh = exampleZh;
        this.level = level;
        this.tags = tags;
        this.sourceOrder = sourceOrder;
    }

    public static WordEntryEntity create(
            Long wordSetId,
            String expression,
            String reading,
            String meaning,
            String partOfSpeech,
            String exampleJp,
            String exampleZh,
            String level,
            List<String> tags,
            Integer sourceOrder
    ) {
        return new WordEntryEntity(
                wordSetId,
                expression,
                reading,
                meaning,
                partOfSpeech,
                exampleJp,
                exampleZh,
                level,
                tags,
                sourceOrder
        );
    }

    public Long getId() {
        return id;
    }

    public Long getWordSetId() {
        return wordSetId;
    }

    public String getExpression() {
        return expression;
    }

    public String getReading() {
        return reading;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public String getExampleJp() {
        return exampleJp;
    }

    public String getExampleZh() {
        return exampleZh;
    }

    public String getLevel() {
        return level;
    }

    public List<String> getTags() {
        return tags;
    }

    public Integer getSourceOrder() {
        return sourceOrder;
    }
}
