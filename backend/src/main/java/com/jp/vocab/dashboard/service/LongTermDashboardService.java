package com.jp.vocab.dashboard.service;

import com.jp.vocab.dashboard.dto.LongTermDashboardResponse;
import com.jp.vocab.dashboard.dto.LongTermLearningSummaryResponse;
import com.jp.vocab.dashboard.dto.LongTermLoadBucketResponse;
import com.jp.vocab.dashboard.dto.LongTermLoadForecastResponse;
import com.jp.vocab.dashboard.dto.LongTermTrendItemResponse;
import com.jp.vocab.shared.auth.CurrentUserService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LongTermDashboardService {

    public static final int DEFAULT_RANGE_DAYS = 90;
    private static final int LAST_7_DAYS = 7;
    private static final int LAST_30_DAYS = 30;
    private static final int FORECAST_7_DAYS = 7;
    private static final int FORECAST_14_DAYS = 14;
    private static final int FORECAST_30_DAYS = 30;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;

    public LongTermDashboardService(
            NamedParameterJdbcTemplate jdbcTemplate,
            CurrentUserService currentUserService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public LongTermDashboardResponse getDashboard(LocalDate targetDate, int rangeDays) {
        Long userId = currentUserService.getCurrentUserId();
        LocalDate trendStartDate = targetDate.minusDays(rangeDays - 1L);
        List<LongTermTrendItemResponse> trend = getTrend(userId, trendStartDate, targetDate);
        List<LocalDate> activeDays = getActiveLearningDays(userId, targetDate);
        List<LongTermDailyLoadItem> futureLoad = getFutureLoad(userId, targetDate.plusDays(1), targetDate.plusDays(FORECAST_30_DAYS));

        return new LongTermDashboardResponse(
                buildSummary(targetDate, rangeDays, activeDays, trend),
                trend,
                buildLoadForecast(futureLoad)
        );
    }

    private List<LongTermTrendItemResponse> getTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                with days as (
                    select generate_series(cast(:startDate as date), cast(:endDate as date), interval '1 day')::date as day
                ),
                word_reviewed as (
                    select
                        rl.reviewed_at::date as day,
                        count(*) as word_reviews
                    from review_log rl
                    join card_instance ci on ci.id = rl.card_instance_id
                    join study_plan sp on sp.id = ci.plan_id
                    where sp.user_id = :userId
                      and rl.reviewed_at::date between :startDate and :endDate
                    group by rl.reviewed_at::date
                ),
                note_reviewed as (
                    select
                        nrl.reviewed_at::date as day,
                        count(*) as note_reviews
                    from note_review_log nrl
                    join note on note.id = nrl.note_id
                    where note.user_id = :userId
                      and nrl.reviewed_at::date between :startDate and :endDate
                    group by nrl.reviewed_at::date
                )
                select
                    days.day,
                    coalesce(word_reviewed.word_reviews, 0) as word_reviews,
                    coalesce(note_reviewed.note_reviews, 0) as note_reviews
                from days
                left join word_reviewed on word_reviewed.day = days.day
                left join note_reviewed on note_reviewed.day = days.day
                order by days.day asc
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> {
            int wordReviews = rs.getInt("word_reviews");
            int noteReviews = rs.getInt("note_reviews");
            return new LongTermTrendItemResponse(
                    rs.getDate("day").toLocalDate(),
                    wordReviews,
                    noteReviews,
                    wordReviews + noteReviews
            );
        });
    }

    private List<LocalDate> getActiveLearningDays(Long userId, LocalDate targetDate) {
        String sql = """
                select active_day
                from (
                    select rl.reviewed_at::date as active_day
                    from review_log rl
                    join card_instance ci on ci.id = rl.card_instance_id
                    join study_plan sp on sp.id = ci.plan_id
                    where sp.user_id = :userId
                      and rl.reviewed_at::date <= :targetDate
                    union
                    select nrl.reviewed_at::date as active_day
                    from note_review_log nrl
                    join note on note.id = nrl.note_id
                    where note.user_id = :userId
                      and nrl.reviewed_at::date <= :targetDate
                ) active_days
                order by active_day asc
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("targetDate", targetDate);
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> rs.getDate("active_day").toLocalDate());
    }

    private List<LongTermDailyLoadItem> getFutureLoad(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                with days as (
                    select generate_series(cast(:startDate as date), cast(:endDate as date), interval '1 day')::date as day
                ),
                word_due as (
                    select
                        ci.due_at::date as day,
                        count(*) as word_due
                    from card_instance ci
                    join study_plan sp on sp.id = ci.plan_id
                    where sp.status = 'ACTIVE'
                      and sp.user_id = :userId
                      and ci.status = 'PENDING'
                      and ci.due_at::date between :startDate and :endDate
                    group by ci.due_at::date
                ),
                note_due as (
                    select
                        note.due_at::date as day,
                        count(*) as note_due
                    from note
                    where note.user_id = :userId
                      and note.due_at::date between :startDate and :endDate
                    group by note.due_at::date
                )
                select
                    days.day,
                    coalesce(word_due.word_due, 0) as word_due,
                    coalesce(note_due.note_due, 0) as note_due
                from days
                left join word_due on word_due.day = days.day
                left join note_due on note_due.day = days.day
                order by days.day asc
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new LongTermDailyLoadItem(
                rs.getDate("day").toLocalDate(),
                rs.getInt("word_due"),
                rs.getInt("note_due")
        ));
    }

    private LongTermLearningSummaryResponse buildSummary(
            LocalDate targetDate,
            int rangeDays,
            List<LocalDate> activeDays,
            List<LongTermTrendItemResponse> trend
    ) {
        return new LongTermLearningSummaryResponse(
                targetDate,
                rangeDays,
                calculateCurrentStreak(targetDate, activeDays),
                calculateLongestStreak(activeDays),
                sumTotalReviewsSince(trend, targetDate.minusDays(LAST_7_DAYS - 1L)),
                sumWordReviewsSince(trend, targetDate.minusDays(LAST_7_DAYS - 1L)),
                sumNoteReviewsSince(trend, targetDate.minusDays(LAST_7_DAYS - 1L)),
                sumTotalReviewsSince(trend, targetDate.minusDays(LAST_30_DAYS - 1L)),
                sumWordReviewsSince(trend, targetDate.minusDays(LAST_30_DAYS - 1L)),
                sumNoteReviewsSince(trend, targetDate.minusDays(LAST_30_DAYS - 1L))
        );
    }

    private LongTermLoadForecastResponse buildLoadForecast(List<LongTermDailyLoadItem> futureLoad) {
        return new LongTermLoadForecastResponse(
                sumLoadBucket(futureLoad, FORECAST_7_DAYS),
                sumLoadBucket(futureLoad, FORECAST_14_DAYS),
                sumLoadBucket(futureLoad, FORECAST_30_DAYS)
        );
    }

    private LongTermLoadBucketResponse sumLoadBucket(List<LongTermDailyLoadItem> futureLoad, int days) {
        int wordDue = futureLoad.stream()
                .limit(days)
                .mapToInt(LongTermDailyLoadItem::wordDue)
                .sum();
        int noteDue = futureLoad.stream()
                .limit(days)
                .mapToInt(LongTermDailyLoadItem::noteDue)
                .sum();
        return new LongTermLoadBucketResponse(days, wordDue, noteDue, wordDue + noteDue);
    }

    int calculateCurrentStreak(LocalDate targetDate, List<LocalDate> activeDays) {
        Set<LocalDate> activeDaySet = new HashSet<>(activeDays);
        int streakDays = 0;
        LocalDate cursor = targetDate;
        while (activeDaySet.contains(cursor)) {
            streakDays++;
            cursor = cursor.minusDays(1);
        }
        return streakDays;
    }

    int calculateLongestStreak(List<LocalDate> activeDays) {
        List<LocalDate> sortedDays = activeDays.stream()
                .distinct()
                .sorted()
                .toList();
        int longestStreak = 0;
        int currentStreak = 0;
        LocalDate previousDay = null;
        for (LocalDate activeDay : sortedDays) {
            if (previousDay == null || activeDay.equals(previousDay.plusDays(1))) {
                currentStreak++;
            } else {
                currentStreak = 1;
            }
            longestStreak = Math.max(longestStreak, currentStreak);
            previousDay = activeDay;
        }
        return longestStreak;
    }

    private int sumTotalReviewsSince(List<LongTermTrendItemResponse> trend, LocalDate startDate) {
        return trend.stream()
                .filter(item -> !item.date().isBefore(startDate))
                .mapToInt(LongTermTrendItemResponse::totalReviews)
                .sum();
    }

    private int sumWordReviewsSince(List<LongTermTrendItemResponse> trend, LocalDate startDate) {
        return trend.stream()
                .filter(item -> !item.date().isBefore(startDate))
                .mapToInt(LongTermTrendItemResponse::wordReviews)
                .sum();
    }

    private int sumNoteReviewsSince(List<LongTermTrendItemResponse> trend, LocalDate startDate) {
        return trend.stream()
                .filter(item -> !item.date().isBefore(startDate))
                .mapToInt(LongTermTrendItemResponse::noteReviews)
                .sum();
    }
}

record LongTermDailyLoadItem(
        LocalDate date,
        int wordDue,
        int noteDue
) {
}
