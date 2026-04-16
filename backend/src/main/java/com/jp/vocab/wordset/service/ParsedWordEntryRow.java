package com.jp.vocab.wordset.service;

import java.util.Map;

record ParsedWordEntryRow(
        long lineNumber,
        Map<String, String> values
) {
}
