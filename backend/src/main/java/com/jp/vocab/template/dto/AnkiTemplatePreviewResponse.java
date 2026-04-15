package com.jp.vocab.template.dto;

public record AnkiTemplatePreviewResponse(
        String frontRendered,
        String backRendered,
        String cssRendered
) {
}
