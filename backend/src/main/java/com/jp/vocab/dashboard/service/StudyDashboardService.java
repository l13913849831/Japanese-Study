package com.jp.vocab.dashboard.service;

import com.jp.vocab.dashboard.dto.DashboardOverviewResponse;
import com.jp.vocab.dashboard.dto.DashboardPlanSummaryResponse;
import com.jp.vocab.dashboard.dto.DashboardTrendItemResponse;
import com.jp.vocab.dashboard.dto.StudyDashboardResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class StudyDashboardService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StudyDashboardService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public StudyDashboardResponse getDashboard(LocalDate targetDate) {
        return new StudyDashboardResponse(
                getOverview(targetDate),
                getActivePlans(targetDate),
                getRecentTrend(targetDate.minusDays(6), targetDate)
        );
    }

    private DashboardOverviewResponse getOverview(LocalDate targetDate) {
        LocalDate endExclusive = targetDate.plusDays(1);
        String sql = """
                select
                    (select count(*)
                     from study_plan
                     where status = 'ACTIVE') as active_plan_count,
                    count(ci.id) as total_due_today,
                    coalesce(sum(case when ci.card_type = 'NEW' then 1 else 0 end), 0) as new_due_today,
                    coalesce(sum(case when ci.card_type = 'REVIEW' then 1 else 0 end), 0) as review_due_today,
                    coalesce(sum(case when ci.status = 'PENDING' then 1 else 0 end), 0) as pending_due_today,
                    (
                        select coalesce(count(distinct rl.card_instance_id), 0)
                        from review_log rl
                        join card_instance reviewed_ci on reviewed_ci.id = rl.card_instance_id
                        join study_plan reviewed_sp on reviewed_sp.id = reviewed_ci.plan_id
                        where reviewed_sp.status = 'ACTIVE'
                          and rl.reviewed_at::date = :targetDate
                    ) as reviewed_today
                from study_plan sp
                left join card_instance ci
                    on ci.plan_id = sp.id
                   and ci.status = 'PENDING'
                   and ci.due_at < :endExclusive
                where sp.status = 'ACTIVE'
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("targetDate", targetDate)
                .addValue("endExclusive", endExclusive.atStartOfDay());

        return jdbcTemplate.queryForObject(sql, parameters, (rs, rowNum) -> new DashboardOverviewResponse(
                targetDate,
                rs.getInt("active_plan_count"),
                rs.getInt("total_due_today"),
                rs.getInt("new_due_today"),
                rs.getInt("review_due_today"),
                rs.getInt("pending_due_today"),
                rs.getInt("reviewed_today")
        ));
    }

    private List<DashboardPlanSummaryResponse> getActivePlans(LocalDate targetDate) {
        LocalDate endExclusive = targetDate.plusDays(1);
        String sql = """
                select
                    sp.id as plan_id,
                    sp.name as plan_name,
                    sp.status,
                    sp.start_date,
                    sp.daily_new_count,
                    coalesce(progress.total_cards, 0) as total_cards,
                    coalesce(progress.completed_cards, 0) as completed_cards,
                    case
                        when coalesce(progress.total_cards, 0) = 0 then 0
                        else round(coalesce(progress.completed_cards, 0) * 100.0 / progress.total_cards, 1)
                    end as completion_rate,
                    coalesce(today.total_due, 0) as due_today,
                    coalesce(today.new_today, 0) as new_today,
                    coalesce(today.review_today, 0) as review_today,
                    coalesce(today.pending_today, 0) as pending_today,
                    coalesce(reviewed.reviewed_today, 0) as reviewed_today
                from study_plan sp
                left join (
                    select
                        plan_id,
                        count(*) as total_due,
                        sum(case when card_type = 'NEW' then 1 else 0 end) as new_today,
                        sum(case when card_type = 'REVIEW' then 1 else 0 end) as review_today,
                        sum(case when status = 'PENDING' then 1 else 0 end) as pending_today
                    from card_instance
                    where status = 'PENDING'
                      and due_at < :endExclusive
                    group by plan_id
                ) today on today.plan_id = sp.id
                left join (
                    select
                        plan_id,
                        count(distinct word_entry_id) as total_cards,
                        count(distinct case when review_count > 0 or last_reviewed_at is not null then word_entry_id end) as completed_cards
                    from card_instance
                    group by plan_id
                ) progress on progress.plan_id = sp.id
                left join (
                    select
                        ci.plan_id,
                        count(distinct rl.card_instance_id) as reviewed_today
                    from review_log rl
                    join card_instance ci on ci.id = rl.card_instance_id
                    where rl.reviewed_at::date = :targetDate
                    group by ci.plan_id
                ) reviewed on reviewed.plan_id = sp.id
                where sp.status = 'ACTIVE'
                order by due_today desc, reviewed_today desc, sp.start_date asc, sp.id asc
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("targetDate", targetDate)
                .addValue("endExclusive", endExclusive.atStartOfDay());

        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new DashboardPlanSummaryResponse(
                rs.getLong("plan_id"),
                rs.getString("plan_name"),
                rs.getString("status"),
                rs.getDate("start_date").toLocalDate(),
                rs.getInt("daily_new_count"),
                rs.getInt("total_cards"),
                rs.getInt("completed_cards"),
                rs.getDouble("completion_rate"),
                rs.getInt("due_today"),
                rs.getInt("new_today"),
                rs.getInt("review_today"),
                rs.getInt("pending_today"),
                rs.getInt("reviewed_today")
        ));
    }

    private List<DashboardTrendItemResponse> getRecentTrend(LocalDate startDate, LocalDate endDate) {
        String sql = """
                with days as (
                    select generate_series(cast(:startDate as date), cast(:endDate as date), interval '1 day')::date as day
                ),
                due as (
                    select
                        ci.due_at::date as day,
                        sum(case when ci.card_type = 'NEW' then 1 else 0 end) as new_cards,
                        sum(case when ci.card_type = 'REVIEW' then 1 else 0 end) as review_cards
                    from card_instance ci
                    join study_plan sp on sp.id = ci.plan_id
                    where sp.status = 'ACTIVE'
                      and ci.status = 'PENDING'
                      and ci.due_at::date between :startDate and :endDate
                    group by ci.due_at::date
                ),
                reviewed as (
                    select
                        rl.reviewed_at::date as day,
                        count(distinct rl.card_instance_id) as reviewed_cards
                    from review_log rl
                    join card_instance ci on ci.id = rl.card_instance_id
                    join study_plan sp on sp.id = ci.plan_id
                    where sp.status = 'ACTIVE'
                      and rl.reviewed_at::date between :startDate and :endDate
                    group by rl.reviewed_at::date
                )
                select
                    days.day,
                    coalesce(due.new_cards, 0) as new_cards,
                    coalesce(due.review_cards, 0) as review_cards,
                    coalesce(reviewed.reviewed_cards, 0) as reviewed_cards
                from days
                left join due on due.day = days.day
                left join reviewed on reviewed.day = days.day
                order by days.day asc
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new DashboardTrendItemResponse(
                rs.getDate("day").toLocalDate(),
                rs.getInt("new_cards"),
                rs.getInt("review_cards"),
                rs.getInt("reviewed_cards")
        ));
    }
}
