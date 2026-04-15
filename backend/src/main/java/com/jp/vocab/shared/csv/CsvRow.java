package com.jp.vocab.shared.csv;

import java.util.Map;

public record CsvRow(
        long lineNumber,
        Map<String, String> values
) {
}
