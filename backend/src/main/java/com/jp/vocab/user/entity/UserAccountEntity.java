package com.jp.vocab.user.entity;

import com.jp.vocab.shared.auth.UserRole;
import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_account")
public class UserAccountEntity extends AuditableEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    protected UserAccountEntity() {
    }

    public static UserAccountEntity create(String displayName) {
        return create(displayName, UserRole.USER);
    }

    public static UserAccountEntity create(String displayName, String role) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.displayName = displayName;
        entity.status = STATUS_ACTIVE;
        entity.role = UserRole.normalize(role);
        return entity;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void updateRole(String role) {
        this.role = UserRole.normalize(role);
    }

    public void disable() {
        status = STATUS_DISABLED;
    }

    public void enable() {
        status = STATUS_ACTIVE;
    }

    public void restoreProfile(String displayName, String status) {
        this.displayName = displayName;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStatus() {
        return status;
    }

    public String getRole() {
        return role;
    }
}
