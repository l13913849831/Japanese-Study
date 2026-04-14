package com.jp.vocab.shared.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(false, null, error, OffsetDateTime.now());
    }
}
