package com.jp.vocab.wordset.service;

import java.util.List;

record ImportAnalysis(
        List<AnalyzedImportRow> rows,
        int readyCount,
        int duplicateCount,
        int errorCount
) {
}
