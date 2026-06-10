package com.jp.vocab.dashboard.dto;

public record LongTermLoadBucketResponse(
        int days,
        int wordDue,
        int noteDue,
        int totalDue
) {
}
