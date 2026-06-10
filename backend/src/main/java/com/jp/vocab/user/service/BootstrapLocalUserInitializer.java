package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AuthProvider;
import com.jp.vocab.shared.auth.LearningOrder;
import com.jp.vocab.shared.auth.UserRole;
import com.jp.vocab.shared.config.AuthBootstrapProperties;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.entity.UserSettingEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserIdentityRepository;
import com.jp.vocab.user.repository.UserSettingRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class BootstrapLocalUserInitializer implements ApplicationRunner {

    private static final long BOOTSTRAP_USER_ID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BootstrapLocalUserInitializer.class);
    private static final String DEFAULT_USERNAME = "demo";
    private static final String DEFAULT_PASSWORD = "demo123456";

    private final AuthBootstrapProperties properties;
    private final UserAccountRepository userAccountRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapLocalUserInitializer(
            AuthBootstrapProperties properties,
            UserAccountRepository userAccountRepository,
            UserIdentityRepository userIdentityRepository,
            UserSettingRepository userSettingRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userAccountRepository = userAccountRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.userSettingRepository = userSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        logger.info("Bootstrap local user initialization started, enabled={}", properties.isEnabled());
        if (!properties.isEnabled()) {
            logger.info("Bootstrap local user initialization skipped");
            return;
        }

        validateBootstrapConfiguration();
        if (isUsingDefaultCredentials()) {
            logger.warn("Bootstrap local account is enabled with default credentials. Use this only for local development.");
        }

        UserAccountEntity account = userAccountRepository.findById(BOOTSTRAP_USER_ID)
                .orElseGet(() -> userAccountRepository.save(UserAccountEntity.create(
                        normalizeDisplayName(properties.getDisplayName()),
                        properties.getRole()
                )));

        userIdentityRepository.findByUserAccountIdAndProvider(account.getId(), AuthProvider.LOCAL)
                .orElseGet(() -> userIdentityRepository.save(UserIdentityEntity.createLocal(
                        account,
                        normalizeUsername(properties.getUsername()),
                        passwordEncoder.encode(properties.getPassword())
                )));

        userSettingRepository.findById(account.getId())
                .orElseGet(() -> userSettingRepository.save(UserSettingEntity.create(
                        account.getId(),
                        normalizeLearningOrder(properties.getPreferredLearningOrder())
                )));

        if (account.getDisplayName() == null || account.getDisplayName().isBlank()) {
            account.updateDisplayName(normalizeDisplayName(properties.getDisplayName()));
            userAccountRepository.save(account);
        }
        if (!UserRole.normalize(properties.getRole()).equals(account.getRole())) {
            account.updateRole(properties.getRole());
            userAccountRepository.save(account);
        }

        logger.info("Bootstrap local user initialization completed, userId={}", account.getId());
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        return normalized.isEmpty() ? "demo" : normalized;
    }

    private String normalizeDisplayName(String displayName) {
        String normalized = displayName == null ? "" : displayName.trim();
        return normalized.isEmpty() ? "Demo User" : normalized;
    }

    private void validateBootstrapConfiguration() {
        if (properties.getPassword() == null || properties.getPassword().isBlank()) {
            throw new IllegalStateException("Bootstrap local account password must not be blank when bootstrap is enabled");
        }
    }

    private boolean isUsingDefaultCredentials() {
        return DEFAULT_USERNAME.equals(normalizeUsername(properties.getUsername()))
                && DEFAULT_PASSWORD.equals(properties.getPassword());
    }

    private String normalizeLearningOrder(String preferredLearningOrder) {
        String normalized = preferredLearningOrder == null ? "" : preferredLearningOrder.trim().toUpperCase();
        return LearningOrder.NOTE_FIRST.equals(normalized) ? LearningOrder.NOTE_FIRST : LearningOrder.WORD_FIRST;
    }
}
