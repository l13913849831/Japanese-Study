package com.jp.vocab.learninglink.repository;

import com.jp.vocab.learninglink.entity.LearningLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearningLinkRepository extends JpaRepository<LearningLinkEntity, Long> {

    Optional<LearningLinkEntity> findByIdAndUserId(Long id, Long userId);

    Optional<LearningLinkEntity> findByUserIdAndWordEntryIdAndNoteId(Long userId, Long wordEntryId, Long noteId);
}
