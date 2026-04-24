package com.jp.vocab.note.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoteMarkdownParserTest {

    private final NoteMarkdownParser parser = new NoteMarkdownParser();

    @Test
    void shouldKeepUntitledContentAsEditablePreviewItem() {
        NoteMarkdownParser.ParsedMarkdownNotes result = parser.parse("""
                开头说明

                # 第一节
                正文
                """, "H1", List.of("A"));

        assertEquals(2, result.items().size());
        assertEquals("未命名知识点", result.items().getFirst().title());
        assertEquals(List.of("A"), result.items().getFirst().tags());
    }

    @Test
    void shouldBuildBreadcrumbTitleWhenSplitByH1H2() {
        NoteMarkdownParser.ParsedMarkdownNotes result = parser.parse("""
                # Java
                ## Stream
                map / filter
                ## Optional
                null safety
                """, "H1_H2", List.of());

        assertEquals(2, result.items().size());
        assertEquals("Java / Stream", result.items().get(0).title());
        assertEquals("Java / Optional", result.items().get(1).title());
    }
}
