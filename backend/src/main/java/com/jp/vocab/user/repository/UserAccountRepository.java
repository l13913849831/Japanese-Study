package com.jp.vocab.user.repository;

import com.jp.vocab.user.entity.UserAccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {

    @Query("""
            select account
            from UserAccountEntity account
            where (:keyword is null
                or lower(account.displayName) like :keyword
                or exists (
                    select identity.id
                    from UserIdentityEntity identity
                    where identity.userAccount = account
                        and identity.provider = 'LOCAL'
                        and lower(identity.providerSubject) like :keyword
                ))
                and (:status is null or account.status = :status)
                and (:role is null or account.role = :role)
            """)
    Page<UserAccountEntity> searchAdminUsers(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("role") String role,
            Pageable pageable
    );
}
