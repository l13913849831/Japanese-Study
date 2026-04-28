package com.jp.vocab.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.bootstrap")
public class AuthBootstrapProperties {

    private boolean enabled = true;

    private String username = "demo";

    private String password = "demo123456";

    private String displayName = "Demo User";

    private String preferredLearningOrder = "WORD_FIRST";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPreferredLearningOrder() {
        return preferredLearningOrder;
    }

    public void setPreferredLearningOrder(String preferredLearningOrder) {
        this.preferredLearningOrder = preferredLearningOrder;
    }
}
