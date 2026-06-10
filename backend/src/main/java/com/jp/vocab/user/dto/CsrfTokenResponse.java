package com.jp.vocab.user.dto;

public record CsrfTokenResponse(
        String headerName,
        String parameterName,
        String token
) {
}
