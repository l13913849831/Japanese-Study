package com.jp.vocab.card.dto;

public record TodayCardResponse(
        Long id,
        Long planId,
        Long wordEntryId,
        String cardType,
        Integer sequenceNo,
        Integer stageNo,
        String dueDate,
        String status,
        String expression,
        String reading,
        String meaning,
        String exampleJp,
        String exampleZh
) {
}
