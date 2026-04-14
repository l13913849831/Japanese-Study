package com.jp.vocab.wordset.repository;

import com.jp.vocab.wordset.entity.WordEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordEntryRepository extends JpaRepository<WordEntryEntity, Long> {
}
