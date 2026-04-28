package com.jp.vocab.exportjob.service;

import com.jp.vocab.card.dto.GeneratedCardRecord;
import com.jp.vocab.card.service.CardQueryService;
import com.jp.vocab.exportjob.dto.CreateExportJobRequest;
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
import java.util.Map;
import java.util.Set;

@Service
public class ExportJobService {

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
        StudyPlanEntity plan = studyPlanAccessService.getOwnedPlan(request.planId());

        List<GeneratedCardRecord> cards = cardQueryService.queryDetailedCards(plan.getId(), request.targetDate());
        String exportType = request.exportType();
        if (!Set.of("ANKI_CSV", "ANKI_TSV", "MARKDOWN").contains(exportType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "exportType is invalid");
        }

        String extension = switch (exportType) {
            case "ANKI_CSV" -> "csv";
            case "ANKI_TSV" -> "tsv";
            case "MARKDOWN" -> "md";
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "exportType is invalid");
        };

        String delimiter = "ANKI_TSV".equals(exportType) ? "\t" : ",";
        String fileName = buildFileName(plan, request.targetDate(), extension);
        Path exportDir = Path.of(exportProperties.getBaseDir());
        Path outputFile = exportDir.resolve(fileName);

        try {
            Files.createDirectories(exportDir);
            String content = "MARKDOWN".equals(exportType)
                    ? buildMarkdown(plan, request.targetDate(), cards)
                    : buildDelimited(plan, request.targetDate(), cards, delimiter);

            Files.writeString(
                    outputFile,
                    content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.EXPORT_ERROR, "Failed to generate export file");
        }

        ExportJobEntity saved = exportJobRepository.save(ExportJobEntity.create(
                plan.getId(),
                exportType,
                request.targetDate(),
                fileName,
                outputFile.toAbsolutePath().toString(),
                "SUCCESS"
        ));

        return ExportJobResponse.from(saved);
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

    private String buildMarkdown(StudyPlanEntity plan, java.time.LocalDate targetDate, List<GeneratedCardRecord> cards) {
        MarkdownTemplateEntity template = plan.getMdTemplateId() != null
                ? templateAccessService.getAccessibleMarkdownTemplate(plan.getMdTemplateId())
                : templateAccessService.getDefaultMarkdownTemplate();

        templateEngine.validate(
                template.getTemplateContent(),
                Set.of("date", "planName", "expression", "reading", "meaning", "partOfSpeech", "exampleJp", "exampleZh", "tags", "dueDate"),
                Set.of("newCards", "reviewCards")
        );

        Map<String, Object> context = Map.of(
                "date", targetDate.toString(),
                "planName", plan.getName(),
                "newCards", cards.stream().filter(card -> "NEW".equals(card.cardType())).map(this::toTemplateMap).toList(),
                "reviewCards", cards.stream().filter(card -> "REVIEW".equals(card.cardType())).map(this::toTemplateMap).toList()
        );
        return templateEngine.render(template.getTemplateContent(), context);
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
}
