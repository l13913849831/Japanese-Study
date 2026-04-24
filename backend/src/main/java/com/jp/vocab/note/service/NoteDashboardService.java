package com.jp.vocab.note.service;

import com.jp.vocab.note.dto.NoteDashboardMasteryItemResponse;
import com.jp.vocab.note.dto.NoteDashboardOverviewResponse;
import com.jp.vocab.note.dto.NoteDashboardResponse;
import com.jp.vocab.note.dto.NoteDashboardTrendItemResponse;
import com.jp.vocab.note.dto.RecentNoteItemResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class NoteDashboardService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NoteDashboardService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public NoteDashboardResponse getDashboard(LocalDate targetDate) {
        return new NoteDashboardResponse(
                getOverview(targetDate),
                getMasteryDistribution(),
                getRecentTrend(targetDate.minusDays(6), targetDate),
                getRecentNotes()
        );
    }

    private NoteDashboardOverviewResponse getOverview(LocalDate targetDate) {
        String sql = """
                select
                    count(*) as total_notes,
                    count(*) filter (
                        where due_at < cast(:targetDate as date) + interval '1 day'
                    ) as due_today,
                    count(*) filter (where review_count > 0) as reviewed_notes
                from note
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("targetDate", targetDate);
        return jdbcTemplate.queryForObject(sql, parameters, (rs, rowNum) -> new NoteDashboardOverviewResponse(
                targetDate,
                rs.getInt("due_today"),
                rs.getInt("total_notes"),
                rs.getInt("reviewed_notes")
        ));
    }

    private List<NoteDashboardMasteryItemResponse> getMasteryDistribution() {
        String sql = """
                select mastery_status, count(*) as item_count
                from note
                group by mastery_status
                order by case mastery_status
                    when 'UNSTARTED' then 1
                    when 'LEARNING' then 2
                    when 'CONSOLIDATING' then 3
                    when 'MASTERED' then 4
                    else 99
                end
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new NoteDashboardMasteryItemResponse(
                rs.getString("mastery_status"),
                rs.getInt("item_count")
        ));
    }

    private List<NoteDashboardTrendItemResponse> getRecentTrend(LocalDate startDate, LocalDate endDate) {
        String sql = """
                with days as (
                    select generate_series(cast(:startDate as date), cast(:endDate as date), interval '1 day')::date as day
                ),
                reviewed as (
                    select
                        (reviewed_at at time zone 'UTC')::date as day,
                        count(*) as reviewed_notes
                    from note_review_log
                    where (reviewed_at at time zone 'UTC')::date between :startDate and :endDate
                    group by (reviewed_at at time zone 'UTC')::date
                )
                select
                    days.day,
                    coalesce(reviewed.reviewed_notes, 0) as reviewed_notes
                from days
                left join reviewed on reviewed.day = days.day
                order by days.day asc
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new NoteDashboardTrendItemResponse(
                rs.getDate("day").toLocalDate(),
                rs.getInt("reviewed_notes")
        ));
    }

    private List<RecentNoteItemResponse> getRecentNotes() {
        String sql = """
                select id, title, tags, mastery_status, created_at
                from note
                order by created_at desc, id desc
                limit 5
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RecentNoteItemResponse(
                rs.getLong("id"),
                rs.getString("title"),
                NoteDashboardJdbcSupport.parseTags(rs.getString("tags")),
                rs.getString("mastery_status"),
                rs.getObject("created_at", java.time.OffsetDateTime.class)
        ));
    }
}
