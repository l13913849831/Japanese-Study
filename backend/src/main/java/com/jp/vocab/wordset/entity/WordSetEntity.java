package com.jp.vocab.wordset.entity;

import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "word_set")
public class WordSetEntity extends AuditableEntity {

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

    protected WordSetEntity() {
    }

    private WordSetEntity(String name, String description, String scope, Long ownerUserId) {
        this.name = name;
        this.description = description;
        this.scope = scope;
        this.ownerUserId = ownerUserId;
    }

    public static WordSetEntity createUserOwned(String name, String description, Long ownerUserId) {
        return new WordSetEntity(name, description, "USER", ownerUserId);
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
}
