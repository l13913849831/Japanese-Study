package com.jp.vocab.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jp.vocab.shared.config.WechatMiniappProperties;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class WechatMiniappClient {

    private static final int SUCCESS_CODE = 0;

    private final RestClient restClient;
    private final WechatMiniappProperties properties;

    public WechatMiniappClient(RestClient.Builder restClientBuilder, WechatMiniappProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public WechatMiniappSession exchangeCode(String code) {
        validateConfigured();

        try {
            WechatCodeSessionResponse response = restClient.get()
                    .uri(
                            properties.getCodeToSessionUrl()
                                    + "?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code",
                            properties.getAppId(),
                            properties.getAppSecret(),
                            code
                    )
                    .retrieve()
                    .body(WechatCodeSessionResponse.class);

            if (response == null || response.hasError() || response.openid() == null || response.openid().isBlank()) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "WeChat login failed");
            }

            return new WechatMiniappSession(response.openid());
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "WeChat login failed");
        }
    }

    private void validateConfigured() {
        if (properties.getAppId() == null
                || properties.getAppId().isBlank()
                || properties.getAppSecret() == null
                || properties.getAppSecret().isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "WeChat miniapp auth is not configured");
        }
    }

    private record WechatCodeSessionResponse(
            String openid,
            @JsonProperty("session_key")
            String sessionKey,
            String unionid,
            Integer errcode,
            String errmsg
    ) {
        private boolean hasError() {
            return errcode != null && errcode != SUCCESS_CODE;
        }
    }
}
