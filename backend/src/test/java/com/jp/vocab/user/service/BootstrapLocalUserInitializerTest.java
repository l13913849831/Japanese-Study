package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AuthProvider;
import com.jp.vocab.shared.auth.LearningOrder;
import com.jp.vocab.shared.config.AuthBootstrapProperties;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.entity.UserSettingEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserIdentityRepository;
import com.jp.vocab.user.repository.UserSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapLocalUserInitializerTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private UserSettingRepository userSettingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthBootstrapProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AuthBootstrapProperties();
    }

    @Test
    void shouldSkipBootstrapWhenDisabled() {
        properties.setEnabled(false);
        properties.setPassword("");

        newInitializer().run(null);

        verifyNoInteractions(userAccountRepository, userIdentityRepository, userSettingRepository, passwordEncoder);
    }

    @Test
    void shouldRejectBlankPasswordWhenBootstrapEnabled() {
        properties.setEnabled(true);
        properties.setPassword("  ");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> newInitializer().run(null)
        );

        assertEquals("Bootstrap local account password must not be blank when bootstrap is enabled", exception.getMessage());
        verify(userAccountRepository, never()).findById(any());
        verifyNoInteractions(userIdentityRepository, userSettingRepository, passwordEncoder);
    }

    @Test
    void shouldCreateBootstrapAccountWhenEnabled() {
        properties.setEnabled(true);
        properties.setUsername("  ");
        properties.setDisplayName("  ");
        properties.setPassword("secret");
        properties.setPreferredLearningOrder("note_first");

        when(userAccountRepository.findById(1L)).thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccountEntity.class))).thenAnswer(invocation -> {
            UserAccountEntity account = invocation.getArgument(0);
            ReflectionTestUtils.setField(account, "id", 1L);
            return account;
        });
        when(userIdentityRepository.findByUserAccountIdAndProvider(1L, AuthProvider.LOCAL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userIdentityRepository.save(any(UserIdentityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSettingRepository.findById(1L)).thenReturn(Optional.empty());
        when(userSettingRepository.save(any(UserSettingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        newInitializer().run(null);

        ArgumentCaptor<UserAccountEntity> accountCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        assertEquals("Demo User", accountCaptor.getValue().getDisplayName());

        ArgumentCaptor<UserIdentityEntity> identityCaptor = ArgumentCaptor.forClass(UserIdentityEntity.class);
        verify(userIdentityRepository).save(identityCaptor.capture());
        assertEquals("demo", identityCaptor.getValue().getProviderSubject());
        assertEquals("encoded-secret", identityCaptor.getValue().getPasswordHash());

        ArgumentCaptor<UserSettingEntity> settingCaptor = ArgumentCaptor.forClass(UserSettingEntity.class);
        verify(userSettingRepository).save(settingCaptor.capture());
        assertEquals(LearningOrder.NOTE_FIRST, settingCaptor.getValue().getPreferredLearningOrder());
    }

    private BootstrapLocalUserInitializer newInitializer() {
        return new BootstrapLocalUserInitializer(
                properties,
                userAccountRepository,
                userIdentityRepository,
                userSettingRepository,
                passwordEncoder
        );
    }
}
