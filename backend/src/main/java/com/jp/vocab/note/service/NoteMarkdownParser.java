package com.jp.vocab.note.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class NoteMarkdownParser {

    public ParsedMarkdownNotes parse(String markdown, String splitMode, List<String> commonTags) {
        String normalizedMode = normalizeSplitMode(splitMode);
        List<Integer> splitLevels = resolveSplitLevels(normalizedMode);
        List<MarkdownSection> sections = splitIntoSections(markdown, splitLevels);
        List<ParsedMarkdownNote> items = new ArrayList<>();
        int readyCount = 0;
        int errorCount = 0;

        for (int index = 0; index < sections.size(); index++) {
            MarkdownSection section = sections.get(index);
            String title = section.title().isBlank() ? "未命名知识点" : section.title();
            String content = section.content().trim();
            String status = content.isBlank() ? "ERROR" : "READY";
            String message = content.isBlank() ? "content must not be blank" : "Ready to import";
            if ("READY".equals(status)) {
                readyCount++;
            } else {
                errorCount++;
            }

            items.add(new ParsedMarkdownNote(
                    "preview-" + (index + 1),
                    title,
                    content,
                    commonTags,
                    status,
                    message
            ));
        }

        return new ParsedMarkdownNotes(normalizedMode, items, readyCount, errorCount);
    }

    String normalizeSplitMode(String splitMode) {
        String normalized = splitMode == null ? "" : splitMode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "H1", "H1_H2", "ALL" -> normalized;
            default -> "H1";
        };
    }

    private List<Integer> resolveSplitLevels(String splitMode) {
        return switch (splitMode) {
            case "H1_H2" -> List.of(1, 2);
            case "ALL" -> List.of(1, 2, 3, 4, 5, 6);
            default -> List.of(1);
        };
    }

    private List<MarkdownSection> splitIntoSections(String markdown, List<Integer> splitLevels) {
        List<MarkdownSection> sections = new ArrayList<>();
        List<String> stack = new ArrayList<>();
        String currentTitle = "";
        StringBuilder contentBuilder = new StringBuilder();

        for (String line : splitLines(markdown)) {
            HeadingMatch headingMatch = matchHeading(line);
            if (headingMatch != null && splitLevels.contains(headingMatch.level())) {
                appendCurrentSection(sections, currentTitle, contentBuilder.toString());
                trimStack(stack, headingMatch.level() - 1);
                stack.add(headingMatch.text());
                currentTitle = String.join(" / ", stack);
                contentBuilder.setLength(0);
                continue;
            }

            if (contentBuilder.length() > 0) {
                contentBuilder.append('\n');
            }
            contentBuilder.append(line);
        }

        appendCurrentSection(sections, currentTitle, contentBuilder.toString());
        if (sections.isEmpty()) {
            sections.add(new MarkdownSection("", markdown == null ? "" : markdown.trim()));
        }
        return sections;
    }

    private void appendCurrentSection(List<MarkdownSection> sections, String title, String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (title.isBlank() && normalizedContent.isBlank()) {
            return;
        }
        if (!title.isBlank() && normalizedContent.isBlank()) {
            return;
        }
        sections.add(new MarkdownSection(title, normalizedContent));
    }

    private void trimStack(List<String> stack, int targetSize) {
        while (stack.size() > targetSize) {
            stack.removeLast();
        }
    }

    private List<String> splitLines(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        return Arrays.asList(markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1));
    }

    private HeadingMatch matchHeading(String line) {
        if (line == null) {
            return null;
        }

        int index = 0;
        while (index < line.length() && line.charAt(index) == '#') {
            index++;
        }

        if (index == 0 || index > 6 || index >= line.length() || line.charAt(index) != ' ') {
            return null;
        }

        String text = line.substring(index + 1).trim();
        if (text.isBlank()) {
            return null;
        }
        return new HeadingMatch(index, text);
    }

    public record ParsedMarkdownNotes(
            String splitMode,
            List<ParsedMarkdownNote> items,
            Integer readyCount,
            Integer errorCount
    ) {
    }

    public record ParsedMarkdownNote(
            String itemId,
            String title,
            String content,
            List<String> tags,
            String status,
            String message
    ) {
        public ParsedMarkdownNote {
            tags = tags == null ? List.of() : tags.stream()
                    .map(item -> item == null ? "" : item.trim())
                    .filter(item -> !item.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    private record MarkdownSection(String title, String content) {
    }

    private record HeadingMatch(int level, String text) {
    }
}
