package com.jp.vocab.user.entity;

import com.jp.vocab.shared.auth.LearningOrder;
import com.jp.vocab.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_setting")
public class UserSettingEntity extends AuditableEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "preferred_learning_order", nullable = false, length = 32)
    private String preferredLearningOrder;

    protected UserSettingEntity() {
    }

    public static UserSettingEntity create(Long userId, String preferredLearningOrder) {
        UserSettingEntity entity = new UserSettingEntity();
        entity.userId = userId;
        entity.preferredLearningOrder = preferredLearningOrder == null || preferredLearningOrder.isBlank()
                ? LearningOrder.WORD_FIRST
                : preferredLearningOrder;
        return entity;
    }

    public void updatePreferredLearningOrder(String preferredLearningOrder) {
        this.preferredLearningOrder = preferredLearningOrder;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPreferredLearningOrder() {
        return preferredLearningOrder;
    }
}
