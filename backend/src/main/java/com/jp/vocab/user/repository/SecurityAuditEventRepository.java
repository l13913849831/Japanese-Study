package com.jp.vocab.user.repository;

import com.jp.vocab.user.entity.SecurityAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEventEntity, Long> {
}
