package com.jp.vocab.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.wechat-miniapp")
public class WechatMiniappProperties {

    private String appId = "";

    private String appSecret = "";

    private String codeToSessionUrl = "https://api.weixin.qq.com/sns/jscode2session";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getCodeToSessionUrl() {
        return codeToSessionUrl;
    }

    public void setCodeToSessionUrl(String codeToSessionUrl) {
        this.codeToSessionUrl = codeToSessionUrl;
    }
}
