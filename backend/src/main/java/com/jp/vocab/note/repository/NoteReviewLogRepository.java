package com.jp.vocab.note.repository;

import com.jp.vocab.note.entity.NoteReviewLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteReviewLogRepository extends JpaRepository<NoteReviewLogEntity, Long> {

    List<NoteReviewLogEntity> findByNoteIdOrderByReviewedAtDescIdDesc(Long noteId);

    List<NoteReviewLogEntity> findByNoteIdInOrderByNoteIdAscReviewedAtAscIdAsc(List<Long> noteIds);
}
