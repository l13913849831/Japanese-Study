package com.jp.vocab.exportjob.service;

import com.jp.vocab.card.dto.GeneratedCardRecord;
import com.jp.vocab.card.service.CardQueryService;
import com.jp.vocab.exportjob.dto.CreateExportJobRequest;
import com.jp.vocab.exportjob.dto.ExportJobPreflightResponse;
import com.jp.vocab.exportjob.dto.ExportJobResponse;
import com.jp.vocab.exportjob.entity.ExportJobEntity;
import com.jp.vocab.exportjob.repository.ExportJobRepository;
import com.jp.vocab.shared.auth.ContentScope;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.config.ExportProperties;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.shared.template.SimpleTemplateEngine;
import com.jp.vocab.studyplan.entity.StudyPlanEntity;
import com.jp.vocab.studyplan.service.StudyPlanAccessService;
import com.jp.vocab.template.entity.MarkdownTemplateEntity;
import com.jp.vocab.template.service.TemplateAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportJobServiceTest {

    @TempDir
    private Path tempDir;

    @Mock
    private ExportJobRepository exportJobRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private StudyPlanAccessService studyPlanAccessService;

    @Mock
    private CardQueryService cardQueryService;

    @Mock
    private TemplateAccessService templateAccessService;

    private ExportJobService exportJobService;

    @BeforeEach
    void setUp() {
        ExportProperties exportProperties = new ExportProperties();
        exportProperties.setBaseDir(tempDir.toString());
        exportJobService = new ExportJobService(
                exportJobRepository,
                currentUserService,
                studyPlanAccessService,
                cardQueryService,
                templateAccessService,
                exportProperties,
                new SimpleTemplateEngine()
        );
    }

    @Test
    void shouldReturnPreflightSummaryWithMarkdownTemplate() {
        LocalDate targetDate = LocalDate.parse("2026-06-10");
        StudyPlanEntity plan = createPlan(10L, "Daily Plan", 7L);
        MarkdownTemplateEntity template = MarkdownTemplateEntity.create(
                "Daily Markdown",
                "Review export",
                ContentScope.SYSTEM,
                null,
                "{{#newCards}}{{expression}}{{/newCards}}{{#reviewCards}}{{expression}}{{/reviewCards}}"
        );
        when(studyPlanAccessService.getOwnedPlan(10L)).thenReturn(plan);
        when(cardQueryService.queryDetailedCards(10L, targetDate)).thenReturn(List.of(
                createCard(1L, "NEW", targetDate, "猫"),
                createCard(2L, "REVIEW", targetDate, "犬")
        ));
        when(templateAccessService.getAccessibleMarkdownTemplate(7L)).thenReturn(template);

        ExportJobPreflightResponse response = exportJobService.preflight(
                new CreateExportJobRequest(10L, "markdown", targetDate)
        );

        assertEquals(10L, response.planId());
        assertEquals("MARKDOWN", response.exportType());
        assertEquals(2, response.totalCards());
        assertEquals(1, response.newCards());
        assertEquals(1, response.reviewCards());
        assertEquals("Daily Markdown", response.markdownTemplateName());
        assertTrue(response.creatable());
    }

    @Test
    void shouldRejectEmptyExportBeforeSavingJob() {
        LocalDate targetDate = LocalDate.parse("2026-06-10");
        StudyPlanEntity plan = createPlan(10L, "Daily Plan", null);
        when(studyPlanAccessService.getOwnedPlan(10L)).thenReturn(plan);
        when(cardQueryService.queryDetailedCards(10L, targetDate)).thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> exportJobService.create(new CreateExportJobRequest(10L, "ANKI_CSV", targetDate))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(exportJobRepository, never()).save(any());
    }

    @Test
    void shouldCreateDelimitedExportFileAfterPrecheck() {
        LocalDate targetDate = LocalDate.parse("2026-06-10");
        StudyPlanEntity plan = createPlan(10L, "Daily Plan", null);
        when(studyPlanAccessService.getOwnedPlan(10L)).thenReturn(plan);
        when(cardQueryService.queryDetailedCards(10L, targetDate)).thenReturn(List.of(
                createCard(1L, "NEW", targetDate, "猫")
        ));
        when(exportJobRepository.save(any())).thenAnswer(invocation -> {
            ExportJobEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 99L);
            return entity;
        });

        ExportJobResponse response = exportJobService.create(
                new CreateExportJobRequest(10L, "ANKI_CSV", targetDate)
        );

        assertEquals(99L, response.id());
        assertEquals("SUCCESS", response.status());
        assertTrue(Files.exists(Path.of(response.filePath())));
    }

    private StudyPlanEntity createPlan(Long id, String name, Long mdTemplateId) {
        StudyPlanEntity plan = StudyPlanEntity.create(
                name,
                1L,
                1L,
                LocalDate.parse("2026-06-01"),
                10,
                List.of(0),
                null,
                mdTemplateId
        );
        ReflectionTestUtils.setField(plan, "id", id);
        return plan;
    }

    private GeneratedCardRecord createCard(Long id, String cardType, LocalDate dueDate, String expression) {
        return new GeneratedCardRecord(
                id,
                10L,
                100L + id,
                cardType,
                id.intValue(),
                1,
                dueDate,
                "PENDING",
                expression,
                "reading",
                "meaning",
                "noun",
                "example jp",
                "example zh",
                List.of("tag")
        );
    }
}
