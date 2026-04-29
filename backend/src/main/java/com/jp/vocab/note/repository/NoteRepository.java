package com.jp.vocab.note.repository;

import com.jp.vocab.note.entity.NoteEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<NoteEntity, Long> {

    List<NoteEntity> findByUserId(Long userId, Sort sort);

    List<NoteEntity> findByUserIdOrderByIdAsc(Long userId);

    List<NoteEntity> findByUserIdAndDueAtBeforeOrderByDueAtAscIdAsc(Long userId, OffsetDateTime endExclusive);

    Optional<NoteEntity> findByIdAndUserId(Long id, Long userId);

    long countByNoteSource_Id(Long noteSourceId);
}
