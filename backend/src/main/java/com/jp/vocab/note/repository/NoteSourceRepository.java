package com.jp.vocab.note.repository;

import com.jp.vocab.note.entity.NoteSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteSourceRepository extends JpaRepository<NoteSourceEntity, Long> {
}
