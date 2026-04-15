package com.jp.vocab.shared.csv;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SimpleCsvParser {

    public List<CsvRow> parse(String content) {
        List<String> lines = content.lines().toList();
        if (lines.isEmpty()) {
            return List.of();
        }

        List<String> headers = parseLine(lines.getFirst());
        List<CsvRow> rows = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            List<String> values = parseLine(line);
            Map<String, String> mapped = new LinkedHashMap<>();
            for (int headerIndex = 0; headerIndex < headers.size(); headerIndex++) {
                String header = headers.get(headerIndex);
                String value = headerIndex < values.size() ? values.get(headerIndex) : "";
                mapped.put(header, value);
            }

            rows.add(new CsvRow(i + 1L, mapped));
        }

        return rows;
    }

    private List<String> parseLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        cells.add(current.toString().trim());
        return cells;
    }
}
