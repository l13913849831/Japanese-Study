package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AppUserPrincipal;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.auth.LearningOrder;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.UpdateUserSettingsRequest;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserSettingEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserProfileService {

    private final CurrentUserService currentUserService;
    private final UserAccountRepository userAccountRepository;
    private final UserSettingRepository userSettingRepository;

    public UserProfileService(
            CurrentUserService currentUserService,
            UserAccountRepository userAccountRepository,
            UserSettingRepository userSettingRepository
    ) {
        this.currentUserService = currentUserService;
        this.userAccountRepository = userAccountRepository;
        this.userSettingRepository = userSettingRepository;
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
}
