package com.jp.vocab.user.entity;

import com.jp.vocab.shared.auth.AuthProvider;
import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_identity")
public class UserIdentityEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "last_failed_login_at")
    private OffsetDateTime lastFailedLoginAt;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity userAccount;

    protected UserIdentityEntity() {
    }

    public static UserIdentityEntity createLocal(
            UserAccountEntity userAccount,
            String username,
            String passwordHash
    ) {
        UserIdentityEntity entity = new UserIdentityEntity();
        entity.userAccount = userAccount;
        entity.provider = AuthProvider.LOCAL;
        entity.providerSubject = username;
        entity.passwordHash = passwordHash;
        return entity;
    }

    public void recordFailedLogin(OffsetDateTime failedAt, int maxFailedAttempts, Duration lockDuration) {
        failedLoginCount += 1;
        lastFailedLoginAt = failedAt;
        if (failedLoginCount >= Math.max(1, maxFailedAttempts)) {
            lockedUntil = failedAt.plus(lockDuration == null ? Duration.ofMinutes(15) : lockDuration);
        }
    }

    public void clearLoginFailureState() {
        failedLoginCount = 0;
        lastFailedLoginAt = null;
        lockedUntil = null;
    }

    public boolean hasLoginFailureState() {
        return failedLoginCount > 0 || lastFailedLoginAt != null || lockedUntil != null;
    }

    public boolean isLoginLocked(OffsetDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean hasExpiredLoginLock(OffsetDateTime now) {
        return lockedUntil != null && !lockedUntil.isAfter(now);
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public OffsetDateTime getLastFailedLoginAt() {
        return lastFailedLoginAt;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public UserAccountEntity getUserAccount() {
        return userAccount;
    }
}
