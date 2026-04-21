package com.jp.vocab.wordset.service;

import com.jp.vocab.wordset.dto.WordEntryImportFieldMapping;

import java.util.List;

record ParsedImportPayload(
        String sourceType,
        List<WordEntryImportFieldMapping> fieldMappings,
        List<ParsedWordEntryRow> rows
) {
}
