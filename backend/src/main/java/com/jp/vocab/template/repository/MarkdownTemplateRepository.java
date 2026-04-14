package com.jp.vocab.template.repository;

import com.jp.vocab.template.entity.MarkdownTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkdownTemplateRepository extends JpaRepository<MarkdownTemplateEntity, Long> {
}
