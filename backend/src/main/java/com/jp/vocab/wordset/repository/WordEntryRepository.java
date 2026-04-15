package com.jp.vocab.wordset.repository;

import com.jp.vocab.wordset.entity.WordEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordEntryRepository extends JpaRepository<WordEntryEntity, Long> {

    Page<WordEntryEntity> findByWordSetId(Long wordSetId, Pageable pageable);

    List<WordEntryEntity> findByWordSetIdOrderBySourceOrderAsc(Long wordSetId);
}
