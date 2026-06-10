package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AppUserPrincipal;
import com.jp.vocab.shared.auth.UserRole;
import com.jp.vocab.shared.config.MobileSessionProperties;
import com.jp.vocab.user.entity.MobileSessionEntity;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.repository.MobileSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class MobileSessionService {

    private static final int TOKEN_BYTES = 32;

    private final MobileSessionRepository mobileSessionRepository;
    private final MobileSessionProperties mobileSessionProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public MobileSessionService(
            MobileSessionRepository mobileSessionRepository,
            MobileSessionProperties mobileSessionProperties
    ) {
        this.mobileSessionRepository = mobileSessionRepository;
        this.mobileSessionProperties = mobileSessionProperties;
    }

    @Transactional
    public MobileSessionToken issueToken(UserAccountEntity account) {
        OffsetDateTime now = OffsetDateTime.now();
        String token = generateToken();
        OffsetDateTime expiresAt = now.plus(mobileSessionProperties.getTokenTtl());
        mobileSessionRepository.save(MobileSessionEntity.create(account, hashToken(token), now, expiresAt));
        return new MobileSessionToken(token, expiresAt);
    }

    @Transactional
    public Optional<AppUserPrincipal> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        OffsetDateTime now = OffsetDateTime.now();
        Optional<MobileSessionEntity> session = mobileSessionRepository.findByTokenHash(hashToken(rawToken));
        if (session.isEmpty() || !session.get().isActive(now)) {
            return Optional.empty();
        }

        UserAccountEntity account = session.get().getUserAccount();
        if (!UserAccountEntity.STATUS_ACTIVE.equals(account.getStatus())) {
            return Optional.empty();
        }

        session.get().touch(now);
        mobileSessionRepository.save(session.get());
        return Optional.of(new AppUserPrincipal(
                account.getId(),
                mobileUsername(account.getId()),
                account.getDisplayName(),
                "",
                UserRole.USER,
                true
        ));
    }

    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        mobileSessionRepository.findByTokenHash(hashToken(rawToken))
                .filter(session -> session.getRevokedAt() == null)
                .ifPresent(session -> {
                    session.revoke(OffsetDateTime.now());
                    mobileSessionRepository.save(session);
                });
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    public String mobileUsername(Long userId) {
        return "wechat-miniapp:" + userId;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
