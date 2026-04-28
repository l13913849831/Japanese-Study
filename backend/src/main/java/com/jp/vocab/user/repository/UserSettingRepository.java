package com.jp.vocab.user.repository;

import com.jp.vocab.user.entity.UserSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingRepository extends JpaRepository<UserSettingEntity, Long> {
}
