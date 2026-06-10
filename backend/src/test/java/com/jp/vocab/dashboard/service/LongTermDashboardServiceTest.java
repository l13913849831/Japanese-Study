package com.jp.vocab.dashboard.service;

import com.jp.vocab.dashboard.dto.LongTermDashboardResponse;
import com.jp.vocab.dashboard.dto.LongTermTrendItemResponse;
import com.jp.vocab.shared.auth.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LongTermDashboardServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private CurrentUserService currentUserService;

    private LongTermDashboardService longTermDashboardService;

    @BeforeEach
    void setUp() {
        longTermDashboardService = new LongTermDashboardService(jdbcTemplate, currentUserService);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldBuildLongTermSummaryAndForecast() {
        LocalDate targetDate = LocalDate.parse("2026-06-10");
        List<LongTermTrendItemResponse> trend = createTrend(targetDate);
        List<LocalDate> activeDays = List.of(
                targetDate.minusDays(10),
                targetDate.minusDays(9),
                targetDate.minusDays(8),
                targetDate.minusDays(7),
                targetDate.minusDays(2),
                targetDate.minusDays(1),
                targetDate
        );
        List<LongTermDailyLoadItem> futureLoad = createFutureLoad(targetDate);

        when(currentUserService.getCurrentUserId()).thenReturn(7L);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn((List) trend, (List) activeDays, (List) futureLoad);

        LongTermDashboardResponse response = longTermDashboardService.getDashboard(targetDate, 90);

        assertEquals(90, response.summary().rangeDays());
        assertEquals(3, response.summary().currentStreakDays());
        assertEquals(4, response.summary().longestStreakDays());
        assertEquals(7, response.summary().reviewedLast7Days());
        assertEquals(3, response.summary().wordReviewedLast7Days());
        assertEquals(4, response.summary().noteReviewedLast7Days());
        assertEquals(12, response.summary().reviewedLast30Days());
        assertEquals(7, response.summary().wordReviewedLast30Days());
        assertEquals(5, response.summary().noteReviewedLast30Days());
        assertEquals(90, response.trend().size());
        assertEquals(3, response.loadForecast().next7Days().totalDue());
        assertEquals(8, response.loadForecast().next14Days().totalDue());
        assertEquals(17, response.loadForecast().next30Days().totalDue());
    }

    @Test
    void shouldReturnZeroCurrentStreakWhenTargetDateHasNoReview() {
        LocalDate targetDate = LocalDate.parse("2026-06-10");

        int streak = longTermDashboardService.calculateCurrentStreak(
                targetDate,
                List.of(targetDate.minusDays(1), targetDate.minusDays(2))
        );

        assertEquals(0, streak);
    }

    private List<LongTermTrendItemResponse> createTrend(LocalDate targetDate) {
        List<LongTermTrendItemResponse> trend = new ArrayList<>();
        LocalDate startDate = targetDate.minusDays(89);
        for (int index = 0; index < 90; index++) {
            LocalDate date = startDate.plusDays(index);
            int wordReviews = 0;
            int noteReviews = 0;
            if (date.equals(targetDate.minusDays(29))) {
                wordReviews = 4;
                noteReviews = 1;
            }
            if (date.equals(targetDate.minusDays(6))) {
                wordReviews = 2;
                noteReviews = 3;
            }
            if (date.equals(targetDate)) {
                wordReviews = 1;
                noteReviews = 1;
            }
            trend.add(new LongTermTrendItemResponse(date, wordReviews, noteReviews, wordReviews + noteReviews));
        }
        return trend;
    }

    private List<LongTermDailyLoadItem> createFutureLoad(LocalDate targetDate) {
        List<LongTermDailyLoadItem> futureLoad = new ArrayList<>();
        for (int index = 1; index <= 30; index++) {
            LocalDate date = targetDate.plusDays(index);
            int wordDue = 0;
            int noteDue = 0;
            if (index == 1) {
                wordDue = 2;
                noteDue = 1;
            }
            if (index == 8) {
                wordDue = 3;
                noteDue = 2;
            }
            if (index == 20) {
                wordDue = 4;
                noteDue = 5;
            }
            futureLoad.add(new LongTermDailyLoadItem(date, wordDue, noteDue));
        }
        return futureLoad;
    }
}
