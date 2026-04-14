package com.jp.vocab.template.repository;

import com.jp.vocab.template.entity.AnkiTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnkiTemplateRepository extends JpaRepository<AnkiTemplateEntity, Long> {
}
