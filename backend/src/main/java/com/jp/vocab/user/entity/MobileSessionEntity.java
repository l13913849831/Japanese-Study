package com.jp.vocab.user.entity;

import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "mobile_session")
public class MobileSessionEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity userAccount;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "last_used_at", nullable = false)
    private OffsetDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected MobileSessionEntity() {
    }

    public static MobileSessionEntity create(UserAccountEntity userAccount, String tokenHash, OffsetDateTime now, OffsetDateTime expiresAt) {
        MobileSessionEntity entity = new MobileSessionEntity();
        entity.userAccount = userAccount;
        entity.tokenHash = tokenHash;
        entity.expiresAt = expiresAt;
        entity.lastUsedAt = now;
        return entity;
    }

    public void touch(OffsetDateTime usedAt) {
        lastUsedAt = usedAt;
    }

    public void revoke(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isActive(OffsetDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public UserAccountEntity getUserAccount() {
        return userAccount;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }
}
