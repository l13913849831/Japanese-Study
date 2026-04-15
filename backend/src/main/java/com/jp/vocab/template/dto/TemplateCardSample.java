package com.jp.vocab.template.dto;

import java.util.List;

public record TemplateCardSample(
        String expression,
        String reading,
        String meaning,
        String partOfSpeech,
        String exampleJp,
        String exampleZh,
        List<String> tags,
        String dueDate,
        String planName
) {
}
