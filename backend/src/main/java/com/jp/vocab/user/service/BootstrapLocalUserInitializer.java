package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AuthProvider;
import com.jp.vocab.shared.auth.LearningOrder;
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

@Component
public class BootstrapLocalUserInitializer implements ApplicationRunner {

    private static final long BOOTSTRAP_USER_ID = 1L;

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
        if (!properties.isEnabled()) {
            return;
        }

        UserAccountEntity account = userAccountRepository.findById(BOOTSTRAP_USER_ID)
                .orElseGet(() -> userAccountRepository.save(UserAccountEntity.create(normalizeDisplayName(properties.getDisplayName()))));

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
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        return normalized.isEmpty() ? "demo" : normalized;
    }

    private String normalizeDisplayName(String displayName) {
        String normalized = displayName == null ? "" : displayName.trim();
        return normalized.isEmpty() ? "Demo User" : normalized;
    }

    private String normalizeLearningOrder(String preferredLearningOrder) {
        String normalized = preferredLearningOrder == null ? "" : preferredLearningOrder.trim().toUpperCase();
        return LearningOrder.NOTE_FIRST.equals(normalized) ? LearningOrder.NOTE_FIRST : LearningOrder.WORD_FIRST;
    }
}
