package com.jp.vocab.wordset.service;

import com.jp.vocab.wordset.dto.WordEntryImportFieldMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ImportFieldMappingSupport {

    private static final List<FieldDefinition> FIELD_DEFINITIONS = List.of(
            new FieldDefinition("expression", true, Set.of(
                    "expression", "word", "vocab", "vocabulary", "term", "japanese", "japaneseword", "front",
                    "kanji", "headword", "prompt", "\u5358\u8a9e", "\u65e5\u672c\u8a9e", "\u6f22\u5b57"
            )),
            new FieldDefinition("reading", false, Set.of(
                    "reading", "kana", "yomi", "pronunciation", "furigana", "ruby", "sound",
                    "\u8aad\u307f", "\u304b\u306a", "\u30d5\u30ea\u30ac\u30ca"
            )),
            new FieldDefinition("meaning", true, Set.of(
                    "meaning", "definition", "gloss", "translation", "back", "english", "chinese", "answer", "explanation",
                    "\u610f\u5473", "\u4e2d\u6587", "\u91ca\u4e49", "\u7ffb\u8bd1"
            )),
            new FieldDefinition("partOfSpeech", false, Set.of(
                    "partofspeech", "pos", "speech", "wordclass", "\u54c1\u8a5e", "\u8bcd\u6027"
            )),
            new FieldDefinition("exampleJp", false, Set.of(
                    "examplejp", "exampleja", "sentence", "sentencejp", "jpsentence", "example", "context",
                    "\u4f8b\u6587"
            )),
            new FieldDefinition("exampleZh", false, Set.of(
                    "examplezh", "examplecn", "sentencezh", "sentencecn", "translatedsentence", "translationexample", "sentenceen",
                    "\u4f8b\u53e5", "\u4e2d\u6587\u4f8b\u53e5"
            )),
            new FieldDefinition("level", false, Set.of(
                    "level", "jlpt", "difficulty", "\u7b49\u7ea7", "\u7d1a\u5225"
            )),
            new FieldDefinition("tags", false, Set.of(
                    "tag", "tags", "label", "labels", "\u6807\u7b7e", "\u6a19\u7c64"
            ))
    );

    private ImportFieldMappingSupport() {
    }

    static List<WordEntryImportFieldMapping> buildDiagnostics(Collection<String> sourceFields) {
        List<String> rawFields = new ArrayList<>(sourceFields);
        List<WordEntryImportFieldMapping> diagnostics = new ArrayList<>(FIELD_DEFINITIONS.size());

        for (FieldDefinition definition : FIELD_DEFINITIONS) {
            String matchedField = null;
            for (String sourceField : rawFields) {
                if (definition.aliases().contains(normalizeFieldName(sourceField))) {
                    matchedField = sourceField;
                    break;
                }
            }

            boolean mapped = matchedField != null;
            String note;
            if (mapped) {
                String normalizedMatched = normalizeFieldName(matchedField);
                note = normalizedMatched.equals(normalizeFieldName(definition.targetField()))
                        ? "direct match"
                        : "mapped by alias";
            } else if (definition.required()) {
                note = "required field not detected";
            } else {
                note = "optional field not detected";
            }

            diagnostics.add(new WordEntryImportFieldMapping(
                    definition.targetField(),
                    definition.required(),
                    mapped,
                    matchedField,
                    note
            ));
        }

        return diagnostics;
    }

    static Map<String, String> mapValues(Map<String, String> sourceValues) {
        Map<String, String> mapped = new LinkedHashMap<>();

        for (FieldDefinition definition : FIELD_DEFINITIONS) {
            for (Map.Entry<String, String> entry : sourceValues.entrySet()) {
                if (definition.aliases().contains(normalizeFieldName(entry.getKey()))) {
                    mapped.put(definition.targetField(), entry.getValue());
                    break;
                }
            }
        }

        return mapped;
    }

    static String normalizeFieldName(String fieldName) {
        return fieldName == null
                ? ""
                : fieldName.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_\\-:/]+", "");
    }

    private record FieldDefinition(String targetField, boolean required, Set<String> aliases) {
    }
}
