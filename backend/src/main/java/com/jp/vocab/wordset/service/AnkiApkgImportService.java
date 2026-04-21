package com.jp.vocab.wordset.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class AnkiApkgImportService {

    private static final List<String> COLLECTION_FILE_NAMES = List.of("collection.anki21", "collection.anki2");
    private static final char FIELD_SEPARATOR = '\u001F';
    private static final Pattern BREAK_TAG_PATTERN = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SOUND_PATTERN = Pattern.compile("\\[sound:[^\\]]+\\]");
    private static final Pattern CLOZE_PATTERN = Pattern.compile("\\{\\{c\\d+::(.*?)(?:::[^}]*)?}}");
    private static final Pattern KANA_PATTERN = Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\u30FC\\u30FB\\s]+");

    private final ObjectMapper objectMapper;

    public AnkiApkgImportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedImportPayload parse(MultipartFile file) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("apkg-import-");
            Path collectionPath = extractCollectionDatabase(file, tempDir);
            return readNotes(collectionPath);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.IMPORT_ERROR, "Failed to unpack APKG file");
        } catch (SQLException ex) {
            throw new BusinessException(ErrorCode.IMPORT_ERROR, "Failed to read APKG collection database");
        } finally {
            cleanupQuietly(tempDir);
        }
    }

    private Path extractCollectionDatabase(MultipartFile file, Path tempDir) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName().replace('\\', '/');
                int slashIndex = entryName.lastIndexOf('/');
                if (slashIndex >= 0) {
                    entryName = entryName.substring(slashIndex + 1);
                }
                entryName = entryName.toLowerCase(Locale.ROOT);
                if (COLLECTION_FILE_NAMES.contains(entryName)) {
                    Path target = tempDir.resolve(entryName);
                    Files.copy(zipInputStream, target, StandardCopyOption.REPLACE_EXISTING);
                    zipInputStream.closeEntry();
                    return target;
                }
            }
        }

        throw new BusinessException(ErrorCode.IMPORT_ERROR, "APKG collection database not found");
    }

    private ParsedImportPayload readNotes(Path collectionPath) throws SQLException {
        String jdbcUrl = "jdbc:sqlite:" + collectionPath.toAbsolutePath();
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            Map<Long, List<String>> modelFields = loadModelFields(connection);
            return loadNotes(connection, modelFields);
        }
    }

    private Map<Long, List<String>> loadModelFields(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select models from col limit 1");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return Map.of();
            }

            String modelsJson = resultSet.getString("models");
            if (modelsJson == null || modelsJson.isBlank()) {
                return Map.of();
            }

            try {
                JsonNode root = objectMapper.readTree(modelsJson);
                Map<Long, List<String>> modelFields = new LinkedHashMap<>();
                root.fields().forEachRemaining(entry -> {
                    JsonNode fieldNode = entry.getValue().path("flds");
                    if (!fieldNode.isArray()) {
                        return;
                    }

                    List<String> fieldNames = new ArrayList<>();
                    for (JsonNode item : fieldNode) {
                        fieldNames.add(item.path("name").asText(""));
                    }

                    if (!fieldNames.isEmpty()) {
                        try {
                            modelFields.put(Long.parseLong(entry.getKey()), fieldNames);
                        } catch (NumberFormatException ignored) {
                            // Ignore malformed model keys and continue parsing the remaining models.
                        }
                    }
                });
                return modelFields;
            } catch (IOException ex) {
                throw new BusinessException(ErrorCode.IMPORT_ERROR, "Failed to parse Anki model metadata");
            }
        }
    }

    private ParsedImportPayload loadNotes(Connection connection, Map<Long, List<String>> modelFields) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select id, mid, flds, tags from notes order by id");
             ResultSet resultSet = statement.executeQuery()) {
            List<ParsedWordEntryRow> rows = new ArrayList<>();
            LinkedHashSet<String> allFieldNames = new LinkedHashSet<>();
            long lineNumber = 1;
            while (resultSet.next()) {
                long modelId = resultSet.getLong("mid");
                String[] fieldValues = Optional.ofNullable(resultSet.getString("flds"))
                        .orElse("")
                        .split(String.valueOf(FIELD_SEPARATOR), -1);
                List<String> fieldNames = modelFields.getOrDefault(modelId, fallbackFieldNames(fieldValues.length));
                allFieldNames.addAll(fieldNames);
                rows.add(buildRow(lineNumber++, fieldNames, fieldValues, resultSet.getString("tags")));
            }
            return new ParsedImportPayload(
                    "APKG",
                    ImportFieldMappingSupport.buildDiagnostics(allFieldNames),
                    rows
            );
        }
    }

    private ParsedWordEntryRow buildRow(long lineNumber, List<String> fieldNames, String[] fieldValues, String noteTags) {
        List<String> cleanedValues = new ArrayList<>(fieldValues.length);
        for (String fieldValue : fieldValues) {
            cleanedValues.add(cleanField(fieldValue));
        }

        Map<String, String> rawValues = new LinkedHashMap<>();
        for (int index = 0; index < fieldValues.length; index++) {
            String fieldName = index < fieldNames.size() ? fieldNames.get(index) : "field" + (index + 1);
            rawValues.put(fieldName, cleanedValues.get(index));
        }

        Map<String, String> mappedValues = ImportFieldMappingSupport.mapValues(rawValues);

        LinkedHashSet<Integer> usedIndexes = new LinkedHashSet<>();

        String expression = Optional.ofNullable(mappedValues.get("expression")).orElse("");
        Integer expressionIndex = null;
        if (expression.isBlank()) {
            expressionIndex = findFirstNonBlankIndex(cleanedValues, usedIndexes);
            expression = valueAt(cleanedValues, expressionIndex);
            if (expressionIndex != null) {
                usedIndexes.add(expressionIndex);
            }
        }

        String reading = Optional.ofNullable(mappedValues.get("reading")).orElse("");
        Integer readingIndex = null;
        if (reading.isBlank()) {
            readingIndex = findKanaLikeIndex(cleanedValues, usedIndexes);
            reading = valueAt(cleanedValues, readingIndex);
            if (readingIndex != null && !reading.isBlank()) {
                usedIndexes.add(readingIndex);
            }
        }

        String meaning = Optional.ofNullable(mappedValues.get("meaning")).orElse("");
        Integer meaningIndex = null;
        if (meaning.isBlank()) {
            meaningIndex = findFirstNonBlankIndex(cleanedValues, usedIndexes);
            meaning = valueAt(cleanedValues, meaningIndex);
            if (meaningIndex != null) {
                usedIndexes.add(meaningIndex);
            }
        }

        String partOfSpeech = Optional.ofNullable(mappedValues.get("partOfSpeech")).orElse("");
        String exampleJp = Optional.ofNullable(mappedValues.get("exampleJp")).orElse("");
        String exampleZh = Optional.ofNullable(mappedValues.get("exampleZh")).orElse("");
        String level = Optional.ofNullable(mappedValues.get("level")).orElse("");
        String extraTags = Optional.ofNullable(mappedValues.get("tags")).orElse("");

        Map<String, String> values = new LinkedHashMap<>();
        values.put("expression", expression);
        values.put("reading", reading);
        values.put("meaning", meaning);
        values.put("partOfSpeech", partOfSpeech);
        values.put("exampleJp", exampleJp);
        values.put("exampleZh", exampleZh);
        values.put("level", level);
        values.put("tags", mergeTags(noteTags, extraTags));
        return new ParsedWordEntryRow(lineNumber, values);
    }

    private List<String> fallbackFieldNames(int fieldCount) {
        List<String> fields = new ArrayList<>(fieldCount);
        for (int index = 0; index < fieldCount; index++) {
            fields.add("field" + (index + 1));
        }
        return fields;
    }

    private Integer findFirstNonBlankIndex(List<String> values, Set<Integer> excluded) {
        for (int index = 0; index < values.size(); index++) {
            if (excluded.contains(index)) {
                continue;
            }
            if (!values.get(index).isBlank()) {
                return index;
            }
        }
        return null;
    }

    private Integer findKanaLikeIndex(List<String> values, Set<Integer> excluded) {
        for (int index = 0; index < values.size(); index++) {
            if (excluded.contains(index)) {
                continue;
            }
            String value = values.get(index);
            if (!value.isBlank() && KANA_PATTERN.matcher(value).matches()) {
                return index;
            }
        }
        return null;
    }

    private String valueAt(List<String> values, Integer index) {
        if (index == null || index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index);
    }

    private String mergeTags(String noteTags, String extraTags) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (String tag : splitTags(noteTags)) {
            merged.add(tag);
        }
        for (String tag : splitTags(extraTags)) {
            merged.add(tag);
        }
        return String.join(", ", merged);
    }

    private List<String> splitTags(String rawTags) {
        String normalized = Optional.ofNullable(rawTags).orElse("").trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        String normalizedSeparators = normalized
                .replace('\u3001', ',')
                .replace('\uFF0C', ',');

        return Stream.of(normalizedSeparators.split("[,;|/\\s]+"))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }

    private String cleanField(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }

        String value = BREAK_TAG_PATTERN.matcher(rawValue).replaceAll("\n");
        value = SOUND_PATTERN.matcher(value).replaceAll(" ");
        value = uncloakCloze(value);
        value = HTML_TAG_PATTERN.matcher(value).replaceAll(" ");
        value = HtmlUtils.htmlUnescape(value)
                .replace('\u00A0', ' ')
                .replace('\r', '\n');

        List<String> lines = value.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        return String.join("\n", lines).trim();
    }

    private String uncloakCloze(String value) {
        Matcher matcher = CLOZE_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private void cleanupQuietly(Path tempDir) {
        if (tempDir == null) {
            return;
        }

        try (Stream<Path> stream = Files.walk(tempDir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup for temporary APKG extraction files.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup for temporary APKG extraction files.
        }
    }
}
