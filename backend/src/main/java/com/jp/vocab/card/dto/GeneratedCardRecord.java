package com.jp.vocab.card.dto;

import java.time.LocalDate;
import java.util.List;

public record GeneratedCardRecord(
        Long id,
        Long planId,
        Long wordEntryId,
        String cardType,
        Integer sequenceNo,
        Integer stageNo,
        LocalDate dueDate,
        String status,
        String expression,
        String reading,
        String meaning,
        String partOfSpeech,
        String exampleJp,
        String exampleZh,
        List<String> tags
) {
}
