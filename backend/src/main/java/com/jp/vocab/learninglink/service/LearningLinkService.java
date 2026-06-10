package com.jp.vocab.learninglink.service;

import com.jp.vocab.learninglink.dto.CreateLearningLinkRequest;
import com.jp.vocab.learninglink.dto.LearningLinkResponse;
import com.jp.vocab.learninglink.entity.LearningLinkEntity;
import com.jp.vocab.learninglink.repository.LearningLinkRepository;
import com.jp.vocab.note.repository.NoteRepository;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LearningLinkService {

    private static final Set<String> ALLOWED_SOURCES = Set.of("MANUAL", "REVIEW");

    private final LearningLinkRepository learningLinkRepository;
    private final NoteRepository noteRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;

    public LearningLinkService(
            LearningLinkRepository learningLinkRepository,
            NoteRepository noteRepository,
            NamedParameterJdbcTemplate jdbcTemplate,
            CurrentUserService currentUserService
    ) {
        this.learningLinkRepository = learningLinkRepository;
        this.noteRepository = noteRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public LearningLinkResponse create(CreateLearningLinkRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        ensureWordEntryVisible(request.wordEntryId(), userId);
        ensureNoteOwned(request.noteId(), userId);
        String source = normalizeSource(request.source());

        LearningLinkEntity entity = learningLinkRepository
                .findByUserIdAndWordEntryIdAndNoteId(userId, request.wordEntryId(), request.noteId())
                .orElseGet(() -> learningLinkRepository.save(LearningLinkEntity.create(
                        userId,
                        request.wordEntryId(),
                        request.noteId(),
                        source
                )));
        return getOwnedResponse(entity.getId(), userId);
    }

    @Transactional(readOnly = true)
    public List<LearningLinkResponse> listByWordEntry(Long wordEntryId) {
        Long userId = currentUserService.getCurrentUserId();
        ensureWordEntryVisible(wordEntryId, userId);
        return queryResponses(
                """
                        where ll.user_id = :userId
                          and ll.word_entry_id = :wordEntryId
                        """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("wordEntryId", wordEntryId)
        );
    }

    @Transactional(readOnly = true)
    public List<LearningLinkResponse> listByNote(Long noteId) {
        Long userId = currentUserService.getCurrentUserId();
        ensureNoteOwned(noteId, userId);
        return queryResponses(
                """
                        where ll.user_id = :userId
                          and ll.note_id = :noteId
                        """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("noteId", noteId)
        );
    }

    @Transactional
    public void delete(Long linkId) {
        Long userId = currentUserService.getCurrentUserId();
        LearningLinkEntity entity = learningLinkRepository.findByIdAndUserId(linkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Learning link not found: " + linkId));
        learningLinkRepository.delete(entity);
    }

    private LearningLinkResponse getOwnedResponse(Long linkId, Long userId) {
        List<LearningLinkResponse> responses = queryResponses(
                """
                        where ll.user_id = :userId
                          and ll.id = :linkId
                        """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("linkId", linkId)
        );
        if (responses.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Learning link not found: " + linkId);
        }
        return responses.get(0);
    }

    private List<LearningLinkResponse> queryResponses(String whereClause, MapSqlParameterSource parameters) {
        String sql = """
                select
                    ll.id as link_id,
                    ll.word_entry_id,
                    we.expression,
                    we.reading,
                    ll.note_id,
                    ns.title as note_title,
                    ns.tags as note_tags,
                    ll.source,
                    ll.created_at
                from learning_link ll
                join word_entry we on we.id = ll.word_entry_id
                join note n on n.id = ll.note_id
                join note_source ns on ns.id = n.note_source_id
                """ + whereClause + """
                order by ll.created_at desc, ll.id desc
                """;
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new LearningLinkResponse(
                rs.getLong("link_id"),
                rs.getLong("word_entry_id"),
                rs.getString("expression"),
                rs.getString("reading"),
                rs.getLong("note_id"),
                rs.getString("note_title"),
                parseTags(rs.getString("note_tags")),
                rs.getString("source"),
                rs.getObject("created_at", java.time.OffsetDateTime.class)
        ));
    }

    private void ensureWordEntryVisible(Long wordEntryId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from word_entry we
                        join word_set ws on ws.id = we.word_set_id
                        where we.id = :wordEntryId
                          and (
                            ws.scope = 'SYSTEM'
                            or ws.owner_user_id = :userId
                          )
                        """,
                new MapSqlParameterSource()
                        .addValue("wordEntryId", wordEntryId)
                        .addValue("userId", userId),
                Integer.class
        );
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Word entry not found: " + wordEntryId);
        }
    }

    private void ensureNoteOwned(Long noteId, Long userId) {
        if (noteRepository.findByIdAndUserId(noteId, userId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Note not found: " + noteId);
        }
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "REVIEW";
        }
        String normalized = source.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_SOURCES.contains(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "source is invalid");
        }
        return normalized;
    }

    private List<String> parseTags(String rawJson) {
        if (rawJson == null || rawJson.isBlank() || rawJson.equals("[]")) {
            return List.of();
        }

        return Arrays.stream(rawJson.replace("[", "").replace("]", "").replace("\"", "").split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }
}
