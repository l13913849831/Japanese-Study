package com.jp.vocab.user.repository;

import com.jp.vocab.user.entity.UserIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, Long> {

    Optional<UserIdentityEntity> findByProviderAndProviderSubject(String provider, String providerSubject);

    Optional<UserIdentityEntity> findByUserAccountIdAndProvider(Long userId, String provider);

    boolean existsByProviderAndProviderSubject(String provider, String providerSubject);
}
