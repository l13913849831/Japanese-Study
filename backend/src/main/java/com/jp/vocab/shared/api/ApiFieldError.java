package com.jp.vocab.shared.api;

public record ApiFieldError(
        String field,
        String message
) {
}
