package com.jp.vocab.user.repository;

import com.jp.vocab.user.entity.MobileSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MobileSessionRepository extends JpaRepository<MobileSessionEntity, Long> {

    Optional<MobileSessionEntity> findByTokenHash(String tokenHash);
}
