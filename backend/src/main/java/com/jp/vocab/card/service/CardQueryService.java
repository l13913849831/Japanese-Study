package com.jp.vocab.card.service;

import com.jp.vocab.card.dto.CardCalendarItemResponse;
import com.jp.vocab.card.dto.GeneratedCardRecord;
import com.jp.vocab.card.dto.TodayCardResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class CardQueryService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CardQueryService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<TodayCardResponse> getTodayCards(Long planId, LocalDate date) {
        return queryDetailedCards(planId, date).stream()
                .map(card -> new TodayCardResponse(
                        card.id(),
                        card.planId(),
                        card.wordEntryId(),
                        card.cardType(),
                        card.sequenceNo(),
                        card.stageNo(),
                        card.dueDate().toString(),
                        card.status(),
                        card.expression(),
                        card.reading(),
                        card.meaning(),
                        card.exampleJp(),
                        card.exampleZh()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CardCalendarItemResponse> getCalendar(Long planId, LocalDate start, LocalDate end) {
        String sql = """
                select ci.due_at::date as due_date,
                       sum(case when card_type = 'NEW' then 1 else 0 end) as new_cards,
                       sum(case when card_type = 'REVIEW' then 1 else 0 end) as review_cards
                from card_instance ci
                where ci.plan_id = :planId
                  and ci.status = 'PENDING'
                  and ci.due_at::date between :start and :end
                group by ci.due_at::date
                order by ci.due_at::date asc
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("planId", planId)
                .addValue("start", start)
                .addValue("end", end);

        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new CardCalendarItemResponse(
                rs.getDate("due_date").toLocalDate().toString(),
                rs.getInt("new_cards"),
                rs.getInt("review_cards")
        ));
    }

    @Transactional(readOnly = true)
    public List<GeneratedCardRecord> queryDetailedCards(Long planId, LocalDate date) {
        LocalDate nextDate = date.plusDays(1);
        String sql = """
                select ci.id,
                       ci.plan_id,
                       ci.word_entry_id,
                       ci.card_type,
                       ci.sequence_no,
                       ci.stage_no,
                       ci.due_date,
                       ci.status,
                       we.expression,
                       we.reading,
                       we.meaning,
                       we.part_of_speech,
                       we.example_jp,
                       we.example_zh,
                       we.tags
                from card_instance ci
                join word_entry we on we.id = ci.word_entry_id
                where ci.plan_id = :planId
                  and ci.status = 'PENDING'
                  and ci.due_at < :endExclusive
                order by ci.due_at asc, ci.sequence_no asc, ci.stage_no asc, ci.id asc
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("planId", planId)
                .addValue("endExclusive", nextDate.atStartOfDay());

        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new GeneratedCardRecord(
                rs.getLong("id"),
                rs.getLong("plan_id"),
                rs.getLong("word_entry_id"),
                rs.getString("card_type"),
                rs.getInt("sequence_no"),
                rs.getInt("stage_no"),
                rs.getDate("due_date").toLocalDate(),
                rs.getString("status"),
                rs.getString("expression"),
                rs.getString("reading"),
                rs.getString("meaning"),
                rs.getString("part_of_speech"),
                rs.getString("example_jp"),
                rs.getString("example_zh"),
                parseTags(rs.getString("tags"))
        ));
    }

    private List<String> parseTags(String rawJson) {
        if (rawJson == null || rawJson.isBlank() || rawJson.equals("[]")) {
            return List.of();
        }

        return List.of(rawJson.replace("[", "").replace("]", "").replace("\"", "").split(","))
                .stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }
}
