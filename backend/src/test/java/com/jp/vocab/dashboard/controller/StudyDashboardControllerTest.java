package com.jp.vocab.dashboard.controller;

import com.jp.vocab.dashboard.dto.DashboardOverviewResponse;
import com.jp.vocab.dashboard.dto.LongTermDashboardResponse;
import com.jp.vocab.dashboard.dto.LongTermLearningSummaryResponse;
import com.jp.vocab.dashboard.dto.LongTermLoadBucketResponse;
import com.jp.vocab.dashboard.dto.LongTermLoadForecastResponse;
import com.jp.vocab.dashboard.dto.StudyDashboardResponse;
import com.jp.vocab.dashboard.service.LongTermDashboardService;
import com.jp.vocab.dashboard.service.StudyDashboardService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudyDashboardControllerTest {

    private final StudyDashboardService studyDashboardService = mock(StudyDashboardService.class);
    private final LongTermDashboardService longTermDashboardService = mock(LongTermDashboardService.class);
    private final StudyDashboardController controller = new StudyDashboardController(studyDashboardService, longTermDashboardService);

    @Test
    void shouldDelegateDailyDashboardRequest() {
        LocalDate targetDate = LocalDate.parse("2026-06-10");
        StudyDashboardResponse expected = new StudyDashboardResponse(
                new DashboardOverviewResponse(targetDate, 1, 2, 1, 1, 2, 0),
                List.of(),
                List.of()
        );
        when(studyDashboardService.getDashboard(targetDate)).thenReturn(expected);

        StudyDashboardResponse response = controller.getDashboard(targetDate).data();

        assertEquals(expected, response);
        verify(studyDashboardService).getDashboard(targetDate);
    }

    @Test
    void shouldDelegateLongTermDashboardRequest() {
        LocalDate targetDate = LocalDate.parse("2026-06-10");
        LongTermDashboardResponse expected = new LongTermDashboardResponse(
                new LongTermLearningSummaryResponse(targetDate, 90, 3, 8, 10, 6, 4, 20, 12, 8),
                List.of(),
                new LongTermLoadForecastResponse(
                        new LongTermLoadBucketResponse(7, 1, 2, 3),
                        new LongTermLoadBucketResponse(14, 2, 3, 5),
                        new LongTermLoadBucketResponse(30, 4, 6, 10)
                )
        );
        when(longTermDashboardService.getDashboard(targetDate, 90)).thenReturn(expected);

        LongTermDashboardResponse response = controller.getLongTermDashboard(targetDate, 90).data();

        assertEquals(expected, response);
        verify(longTermDashboardService).getDashboard(targetDate, 90);
    }
}
