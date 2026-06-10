package com.jp.vocab.exportjob.service;

import com.jp.vocab.card.dto.GeneratedCardRecord;
import com.jp.vocab.card.service.CardQueryService;
import com.jp.vocab.exportjob.dto.CreateExportJobRequest;
import com.jp.vocab.exportjob.dto.ExportJobPreflightResponse;
import com.jp.vocab.exportjob.dto.ExportJobResponse;
import com.jp.vocab.exportjob.entity.ExportJobEntity;
import com.jp.vocab.exportjob.repository.ExportJobRepository;
import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.config.ExportProperties;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.shared.template.SimpleTemplateEngine;
import com.jp.vocab.studyplan.entity.StudyPlanEntity;
import com.jp.vocab.studyplan.service.StudyPlanAccessService;
import com.jp.vocab.template.entity.MarkdownTemplateEntity;
import com.jp.vocab.template.service.TemplateAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ExportJobService {

    private static final Logger logger = LoggerFactory.getLogger(ExportJobService.class);
    private static final Set<String> EXPORT_TYPES = Set.of("ANKI_CSV", "ANKI_TSV", "MARKDOWN");
    private static final Set<String> MARKDOWN_ALLOWED_VARIABLES = Set.of(
            "date",
            "planName",
            "expression",
            "reading",
            "meaning",
            "partOfSpeech",
            "exampleJp",
            "exampleZh",
            "tags",
            "dueDate"
    );
    private static final Set<String> MARKDOWN_ALLOWED_SECTIONS = Set.of("newCards", "reviewCards");

    private final ExportJobRepository exportJobRepository;
    private final CurrentUserService currentUserService;
    private final StudyPlanAccessService studyPlanAccessService;
    private final CardQueryService cardQueryService;
    private final TemplateAccessService templateAccessService;
    private final ExportProperties exportProperties;
    private final SimpleTemplateEngine templateEngine;

    public ExportJobService(
            ExportJobRepository exportJobRepository,
            CurrentUserService currentUserService,
            StudyPlanAccessService studyPlanAccessService,
            CardQueryService cardQueryService,
            TemplateAccessService templateAccessService,
            ExportProperties exportProperties,
            SimpleTemplateEngine templateEngine
    ) {
        this.exportJobRepository = exportJobRepository;
        this.currentUserService = currentUserService;
        this.studyPlanAccessService = studyPlanAccessService;
        this.cardQueryService = cardQueryService;
        this.templateAccessService = templateAccessService;
        this.exportProperties = exportProperties;
        this.templateEngine = templateEngine;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExportJobResponse> list(int page, int pageSize) {
        Long userId = currentUserService.getCurrentUserId();
        return PageResponse.from(
                exportJobRepository.findByUserId(
                                userId,
                                PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .map(ExportJobResponse::from)
        );
    }

    @Transactional
    public ExportJobResponse create(CreateExportJobRequest request) {
        /*
         * ========================================================================
         * Step 1: Prepare export context
         * ========================================================================
         * Goal:
         * 1) Validate export type, plan ownership, target date data, and template.
         * 2) Stop before file creation when there is nothing to export.
         */
        logger.info("Starting export job context preparation...");

        // 1.1 Build the same checked context used by preflight.
        ExportJobContext context = prepareExportContext(request);

        // 1.2 Reject empty exports before touching the file system.
        ensureExportableCards(context);
        logger.info(
                "Export job context preparation completed, planId={}, exportType={}, targetDate={}, cards={}",
                context.plan().getId(),
                context.exportType(),
                context.targetDate(),
                context.cards().size()
        );

        /*
         * ========================================================================
         * Step 2: Generate export file
         * ========================================================================
         * Data source:
         * 1) Checked plan and cards from Step 1.
         * 2) Checked Markdown template when export type is MARKDOWN.
         */
        logger.info("Starting export file generation...");

        // 2.1 Resolve output path from the checked context.
        String extension = extensionOf(context.exportType());
        String delimiter = "ANKI_TSV".equals(context.exportType()) ? "\t" : ",";
        String fileName = buildFileName(context.plan(), context.targetDate(), extension);
        Path exportDir = Path.of(exportProperties.getBaseDir());
        Path outputFile = exportDir.resolve(fileName);

        try {
            // 2.2 Generate file content and write it atomically for this request.
            Files.createDirectories(exportDir);
            String content = "MARKDOWN".equals(context.exportType())
                    ? buildMarkdown(context)
                    : buildDelimited(context.plan(), context.targetDate(), context.cards(), delimiter);

            Files.writeString(
                    outputFile,
                    content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            logger.warn("Export file generation failed, planId={}, exportType={}, targetDate={}",
                    context.plan().getId(), context.exportType(), context.targetDate());
            throw new BusinessException(ErrorCode.EXPORT_ERROR, "Failed to generate export file. Please check export directory permissions.");
        }
        logger.info("Export file generation completed, fileName={}", fileName);

        ExportJobEntity saved = exportJobRepository.save(ExportJobEntity.create(
                context.plan().getId(),
                context.exportType(),
                context.targetDate(),
                fileName,
                outputFile.toAbsolutePath().toString(),
                "SUCCESS"
        ));

        return ExportJobResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ExportJobPreflightResponse preflight(CreateExportJobRequest request) {
        /*
         * ========================================================================
         * Step 1: Check export readiness
         * ========================================================================
         * Goal:
         * 1) Validate the same plan/card/template prerequisites as create().
         * 2) Return a user-facing summary before creating an export job.
         */
        logger.info("Starting export preflight...");

        // 1.1 Build checked context without writing files or jobs.
        ExportJobContext context = prepareExportContext(request);

        // 1.2 Convert checked context into a summary for the UI.
        ExportJobPreflightResponse response = toPreflightResponse(context);
        logger.info(
                "Export preflight completed, planId={}, exportType={}, targetDate={}, creatable={}",
                response.planId(),
                response.exportType(),
                response.targetDate(),
                response.creatable()
        );
        return response;
    }

    @Transactional(readOnly = true)
    public Resource getDownloadResource(Long exportJobId) {
        ExportJobEntity exportJob = getEntity(exportJobId);
        if (exportJob.getFilePath() == null || exportJob.getFilePath().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Export file not found for job: " + exportJobId);
        }

        Resource resource = new FileSystemResource(exportJob.getFilePath());
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Export file does not exist");
        }
        return resource;
    }

    @Transactional(readOnly = true)
    public ExportJobEntity getEntity(Long exportJobId) {
        return exportJobRepository.findOwnedById(exportJobId, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Export job not found: " + exportJobId));
    }

    private String buildDelimited(StudyPlanEntity plan, java.time.LocalDate targetDate, List<GeneratedCardRecord> cards, String delimiter) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(delimiter, List.of(
                "expression",
                "reading",
                "meaning",
                "partOfSpeech",
                "exampleJp",
                "exampleZh",
                "tags",
                "planName",
                "dueDate"
        ))).append(System.lineSeparator());

        for (GeneratedCardRecord card : cards) {
            builder.append(csvCell(card.expression())).append(delimiter)
                    .append(csvCell(card.reading())).append(delimiter)
                    .append(csvCell(card.meaning())).append(delimiter)
                    .append(csvCell(card.partOfSpeech())).append(delimiter)
                    .append(csvCell(card.exampleJp())).append(delimiter)
                    .append(csvCell(card.exampleZh())).append(delimiter)
                    .append(csvCell(String.join("|", card.tags()))).append(delimiter)
                    .append(csvCell(plan.getName())).append(delimiter)
                    .append(csvCell(targetDate.toString()))
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    private String buildMarkdown(ExportJobContext context) {
        Map<String, Object> renderContext = Map.of(
                "date", context.targetDate().toString(),
                "planName", context.plan().getName(),
                "newCards", context.cards().stream().filter(card -> "NEW".equals(card.cardType())).map(this::toTemplateMap).toList(),
                "reviewCards", context.cards().stream().filter(card -> "REVIEW".equals(card.cardType())).map(this::toTemplateMap).toList()
        );
        return templateEngine.render(context.markdownTemplate().getTemplateContent(), renderContext);
    }

    private Map<String, Object> toTemplateMap(GeneratedCardRecord card) {
        return Map.of(
                "expression", safe(card.expression()),
                "reading", safe(card.reading()),
                "meaning", safe(card.meaning()),
                "partOfSpeech", safe(card.partOfSpeech()),
                "exampleJp", safe(card.exampleJp()),
                "exampleZh", safe(card.exampleZh()),
                "tags", String.join(", ", card.tags()),
                "dueDate", card.dueDate().toString()
        );
    }

    private String buildFileName(StudyPlanEntity plan, java.time.LocalDate targetDate, String extension) {
        String sanitizedPlanName = plan.getName().replaceAll("[^a-zA-Z0-9\\-_]+", "-");
        return sanitizedPlanName + "-" + targetDate.format(DateTimeFormatter.ISO_DATE) + "." + extension;
    }

    private ExportJobContext prepareExportContext(CreateExportJobRequest request) {
        String exportType = normalizeExportType(request.exportType());
        StudyPlanEntity plan = studyPlanAccessService.getOwnedPlan(request.planId());
        List<GeneratedCardRecord> cards = cardQueryService.queryDetailedCards(plan.getId(), request.targetDate());
        MarkdownTemplateEntity markdownTemplate = null;
        if ("MARKDOWN".equals(exportType)) {
            markdownTemplate = resolveAndValidateMarkdownTemplate(plan);
        }
        return new ExportJobContext(plan, exportType, request.targetDate(), cards, markdownTemplate);
    }

    private ExportJobPreflightResponse toPreflightResponse(ExportJobContext context) {
        int newCards = (int) context.cards().stream().filter(card -> "NEW".equals(card.cardType())).count();
        int reviewCards = (int) context.cards().stream().filter(card -> "REVIEW".equals(card.cardType())).count();
        boolean creatable = !context.cards().isEmpty();
        return new ExportJobPreflightResponse(
                context.plan().getId(),
                context.plan().getName(),
                context.exportType(),
                context.targetDate(),
                context.cards().size(),
                newCards,
                reviewCards,
                context.markdownTemplate() == null ? null : context.markdownTemplate().getName(),
                creatable,
                creatable
                        ? "Ready to export " + context.cards().size() + " cards."
                        : "No due cards found for the target date."
        );
    }

    private void ensureExportableCards(ExportJobContext context) {
        if (context.cards().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "No due cards found for target date: " + context.targetDate()
            );
        }
    }

    private MarkdownTemplateEntity resolveAndValidateMarkdownTemplate(StudyPlanEntity plan) {
        MarkdownTemplateEntity template = plan.getMdTemplateId() != null
                ? templateAccessService.getAccessibleMarkdownTemplate(plan.getMdTemplateId())
                : templateAccessService.getDefaultMarkdownTemplate();
        templateEngine.validate(
                template.getTemplateContent(),
                MARKDOWN_ALLOWED_VARIABLES,
                MARKDOWN_ALLOWED_SECTIONS
        );
        return template;
    }

    private String normalizeExportType(String exportType) {
        String normalized = exportType == null ? "" : exportType.trim().toUpperCase(Locale.ROOT);
        if (EXPORT_TYPES.contains(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "exportType is invalid");
    }

    private String extensionOf(String exportType) {
        return switch (exportType) {
            case "ANKI_CSV" -> "csv";
            case "ANKI_TSV" -> "tsv";
            case "MARKDOWN" -> "md";
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "exportType is invalid");
        };
    }

    private String csvCell(String value) {
        String actual = safe(value);
        if (actual.contains(",") || actual.contains("\"") || actual.contains("\n") || actual.contains("\r") || actual.contains("\t")) {
            return "\"" + actual.replace("\"", "\"\"") + "\"";
        }
        return actual;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ExportJobContext(
            StudyPlanEntity plan,
            String exportType,
            java.time.LocalDate targetDate,
            List<GeneratedCardRecord> cards,
            MarkdownTemplateEntity markdownTemplate
    ) {
    }
}
