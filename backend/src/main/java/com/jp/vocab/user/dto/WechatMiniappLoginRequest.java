package com.jp.vocab.user.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatMiniappLoginRequest(
        @NotBlank
        String code
) {
}
