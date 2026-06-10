package com.jp.vocab.dashboard.dto;

public record LongTermLoadForecastResponse(
        LongTermLoadBucketResponse next7Days,
        LongTermLoadBucketResponse next14Days,
        LongTermLoadBucketResponse next30Days
) {
}
