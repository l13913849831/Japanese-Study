package com.jp.vocab.wordset.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SaveWordEntryRequest(
        @NotBlank(message = "expression must not be blank")
        String expression,
        String reading,
        @NotBlank(message = "meaning must not be blank")
        String meaning,
        String partOfSpeech,
        String exampleJp,
        String exampleZh,
        String level,
        List<String> tags
) {
}
