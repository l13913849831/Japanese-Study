package com.jp.vocab.wordset.repository;

import com.jp.vocab.wordset.entity.WordSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordSetRepository extends JpaRepository<WordSetEntity, Long> {
}
