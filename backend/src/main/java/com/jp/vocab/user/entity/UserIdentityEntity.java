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

    public UserAccountEntity getUserAccount() {
        return userAccount;
    }
}
