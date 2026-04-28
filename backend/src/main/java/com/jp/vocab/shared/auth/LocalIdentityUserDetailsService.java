package com.jp.vocab.shared.auth;

import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.repository.UserIdentityRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LocalIdentityUserDetailsService implements UserDetailsService {

    private final UserIdentityRepository userIdentityRepository;

    public LocalIdentityUserDetailsService(UserIdentityRepository userIdentityRepository) {
        this.userIdentityRepository = userIdentityRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserIdentityEntity identity = userIdentityRepository
                .findByProviderAndProviderSubject(AuthProvider.LOCAL, username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Local identity not found: " + username));

        UserAccountEntity account = identity.getUserAccount();
        boolean enabled = UserAccountEntity.STATUS_ACTIVE.equals(account.getStatus());
        return new AppUserPrincipal(
                account.getId(),
                identity.getProviderSubject(),
                account.getDisplayName(),
                identity.getPasswordHash(),
                enabled
        );
    }
}
