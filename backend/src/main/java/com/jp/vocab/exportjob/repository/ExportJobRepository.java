package com.jp.vocab.exportjob.repository;

import com.jp.vocab.exportjob.entity.ExportJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportJobRepository extends JpaRepository<ExportJobEntity, Long> {
}
