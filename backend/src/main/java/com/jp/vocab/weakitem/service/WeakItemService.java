package com.jp.vocab.weakitem.service;

import com.jp.vocab.card.entity.CardInstanceEntity;
import com.jp.vocab.card.repository.CardInstanceRepository;
import com.jp.vocab.note.entity.NoteEntity;
import com.jp.vocab.note.repository.NoteRepository;
import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.weakitem.dto.WeakItemSummaryResponse;
import com.jp.vocab.weakitem.dto.WeakNoteItemResponse;
import com.jp.vocab.weakitem.dto.WeakWordItemResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WeakItemService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CardInstanceRepository cardInstanceRepository;
    private final NoteRepository noteRepository;

    public WeakItemService(
            NamedParameterJdbcTemplate jdbcTemplate,
            CardInstanceRepository cardInstanceRepository,
            NoteRepository noteRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.cardInstanceRepository = cardInstanceRepository;
        this.noteRepository = noteRepository;
    }

    @Transactional(readOnly = true)
    public WeakItemSummaryResponse getSummary() {
        Integer weakWordCount = jdbcTemplate.queryForObject(
                "select count(*) from card_instance where weak_flag = true",
                new MapSqlParameterSource(),
                Integer.class
        );
        Integer weakNoteCount = jdbcTemplate.queryForObject(
                "select count(*) from note where weak_flag = true",
                new MapSqlParameterSource(),
                Integer.class
        );
        return new WeakItemSummaryResponse(
                weakWordCount == null ? 0 : weakWordCount,
                weakNoteCount == null ? 0 : weakNoteCount
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<WeakWordItemResponse> listWeakWords(int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("limit", safePageSize)
                .addValue("offset", (safePage - 1) * safePageSize);

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from card_instance where weak_flag = true",
                new MapSqlParameterSource(),
                Long.class
        );

        String sql = """
                select
                    ci.id as card_id,
                    ci.plan_id,
                    sp.name as plan_name,
                    ci.word_entry_id,
                    we.expression,
                    we.reading,
                    we.meaning,
                    ci.due_date,
                    ci.last_review_rating,
                    ci.weak_marked_at
                from card_instance ci
                join study_plan sp on sp.id = ci.plan_id
                join word_entry we on we.id = ci.word_entry_id
                where ci.weak_flag = true
                order by ci.weak_marked_at desc nulls last, ci.id desc
                limit :limit offset :offset
                """;

        List<WeakWordItemResponse> items = jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new WeakWordItemResponse(
                rs.getLong("card_id"),
                rs.getLong("plan_id"),
                rs.getString("plan_name"),
                rs.getLong("word_entry_id"),
                rs.getString("expression"),
                rs.getString("reading"),
                rs.getString("meaning"),
                rs.getDate("due_date").toLocalDate(),
                rs.getString("last_review_rating"),
                rs.getObject("weak_marked_at", java.time.OffsetDateTime.class)
        ));

        return new PageResponse<>(items, safePage, safePageSize, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public PageResponse<WeakNoteItemResponse> listWeakNotes(int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("limit", safePageSize)
                .addValue("offset", (safePage - 1) * safePageSize);

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from note where weak_flag = true",
                new MapSqlParameterSource(),
                Long.class
        );

        String sql = """
                select id, title, tags, mastery_status, last_review_rating, weak_marked_at
                from note
                where weak_flag = true
                order by weak_marked_at desc nulls last, id desc
                limit :limit offset :offset
                """;

        List<WeakNoteItemResponse> items = jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new WeakNoteItemResponse(
                rs.getLong("id"),
                rs.getString("title"),
                NoteDashboardJdbcSupport.parseTags(rs.getString("tags")),
                rs.getString("mastery_status"),
                rs.getString("last_review_rating"),
                rs.getObject("weak_marked_at", java.time.OffsetDateTime.class)
        ));

        return new PageResponse<>(items, safePage, safePageSize, total == null ? 0 : total);
    }

    @Transactional
    public void dismissWeakWord(Long cardId) {
        CardInstanceEntity entity = cardInstanceRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Card not found: " + cardId));
        entity.clearWeak();
        cardInstanceRepository.save(entity);
    }

    @Transactional
    public void dismissWeakNote(Long noteId) {
        NoteEntity entity = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Note not found: " + noteId));
        entity.clearWeak();
        noteRepository.save(entity);
    }
}
