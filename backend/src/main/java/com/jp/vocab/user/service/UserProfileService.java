package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AppUserPrincipal;
import com.jp.vocab.shared.auth.AuthProvider;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.auth.LearningOrder;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.user.dto.ChangePasswordRequest;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.PasswordChangeResponse;
import com.jp.vocab.user.dto.UpdateUserProfileRequest;
import com.jp.vocab.user.dto.UpdateUserSettingsRequest;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.entity.UserSettingEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserIdentityRepository;
import com.jp.vocab.user.repository.UserSettingRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserProfileService {

    private final CurrentUserService currentUserService;
    private final UserAccountRepository userAccountRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(
            CurrentUserService currentUserService,
            UserAccountRepository userAccountRepository,
            UserIdentityRepository userIdentityRepository,
            UserSettingRepository userSettingRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.currentUserService = currentUserService;
        this.userAccountRepository = userAccountRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.userSettingRepository = userSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUserProfile() {
        AppUserPrincipal principal = currentUserService.getCurrentUser();
        UserAccountEntity account = getUserAccount(principal.getUserId());
        UserSettingEntity setting = userSettingRepository.findById(account.getId())
                .orElse(UserSettingEntity.create(account.getId(), LearningOrder.WORD_FIRST));
        return new CurrentUserResponse(
                account.getId(),
                principal.getUsername(),
                account.getDisplayName(),
                setting.getPreferredLearningOrder()
        );
    }

    @Transactional
    public CurrentUserResponse updateSettings(UpdateUserSettingsRequest request) {
        AppUserPrincipal principal = currentUserService.getCurrentUser();
        UserAccountEntity account = getUserAccount(principal.getUserId());
        String preferredLearningOrder = normalizeLearningOrder(request.preferredLearningOrder());
        UserSettingEntity setting = userSettingRepository.findById(account.getId())
                .orElseGet(() -> UserSettingEntity.create(account.getId(), preferredLearningOrder));
        setting.updatePreferredLearningOrder(preferredLearningOrder);
        userSettingRepository.save(setting);
        return new CurrentUserResponse(
                account.getId(),
                principal.getUsername(),
                account.getDisplayName(),
                preferredLearningOrder
        );
    }

    @Transactional
    public CurrentUserResponse updateProfile(UpdateUserProfileRequest request) {
        AppUserPrincipal principal = currentUserService.getCurrentUser();
        UserAccountEntity account = getUserAccount(principal.getUserId());
        account.updateDisplayName(normalizeDisplayName(request.displayName()));
        userAccountRepository.save(account);
        UserSettingEntity setting = userSettingRepository.findById(account.getId())
                .orElse(UserSettingEntity.create(account.getId(), LearningOrder.WORD_FIRST));
        return new CurrentUserResponse(
                account.getId(),
                principal.getUsername(),
                account.getDisplayName(),
                setting.getPreferredLearningOrder()
        );
    }

    @Transactional
    public PasswordChangeResponse changePassword(ChangePasswordRequest request) {
        AppUserPrincipal principal = currentUserService.getCurrentUser();
        UserIdentityEntity identity = userIdentityRepository
                .findByUserAccountIdAndProvider(principal.getUserId(), AuthProvider.LOCAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Local identity not found"));

        if (!passwordEncoder.matches(request.currentPassword(), identity.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Current password is incorrect");
        }

        validateNewPassword(request.newPassword());
        if (passwordEncoder.matches(request.newPassword(), identity.getPasswordHash())) {
            throw new BusinessException(ErrorCode.CONFLICT, "New password must be different from current password");
        }

        identity.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        userIdentityRepository.save(identity);
        return new PasswordChangeResponse(true);
    }

    private UserAccountEntity getUserAccount(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User account not found: " + userId));
    }

    private String normalizeLearningOrder(String preferredLearningOrder) {
        String normalized = preferredLearningOrder == null
                ? ""
                : preferredLearningOrder.trim().toUpperCase(Locale.ROOT);
        if (LearningOrder.WORD_FIRST.equals(normalized) || LearningOrder.NOTE_FIRST.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "preferredLearningOrder is invalid");
    }

    private String normalizeDisplayName(String displayName) {
        String normalized = displayName == null ? "" : displayName.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "displayName must not be blank");
        }
        if (normalized.length() > 128) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "displayName must be at most 128 characters");
        }
        return normalized;
    }

    private void validateNewPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "newPassword must not be blank");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "newPassword must be between 8 and 72 characters");
        }
    }
}
