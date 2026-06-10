package com.jp.vocab.user.service;

import com.jp.vocab.user.entity.SecurityAuditEventEntity;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.repository.SecurityAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceTest {

    @Mock
    private SecurityAuditEventRepository securityAuditEventRepository;

    private SecurityAuditService securityAuditService;

    @BeforeEach
    void setUp() {
        securityAuditService = new SecurityAuditService(securityAuditEventRepository);
    }

    @Test
    void shouldRecordLoginFailureAuditEvent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        UserIdentityEntity identity = createIdentity();

        securityAuditService.recordLoginFailure(request, identity, "demo");

        ArgumentCaptor<SecurityAuditEventEntity> eventCaptor = ArgumentCaptor.forClass(SecurityAuditEventEntity.class);
        verify(securityAuditEventRepository).save(eventCaptor.capture());

        SecurityAuditEventEntity event = eventCaptor.getValue();
        assertEquals(SecurityAuditEventEntity.EVENT_LOGIN_FAILURE, event.getEventType());
        assertEquals(SecurityAuditEventEntity.OUTCOME_FAILURE, event.getOutcome());
        assertEquals(1L, event.getUserId());
        assertEquals("demo", event.getUsername());
        assertEquals("127.0.0.1", event.getIpAddress());
        assertEquals("JUnit", event.getUserAgent());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void shouldPreferForwardedIpAndTruncateLongUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "192.168.1.10, 10.0.0.2");
        request.addHeader("User-Agent", "x".repeat(600));

        securityAuditService.recordLoginLocked(request, createIdentity(), "demo");

        ArgumentCaptor<SecurityAuditEventEntity> eventCaptor = ArgumentCaptor.forClass(SecurityAuditEventEntity.class);
        verify(securityAuditEventRepository).save(eventCaptor.capture());

        SecurityAuditEventEntity event = eventCaptor.getValue();
        assertEquals(SecurityAuditEventEntity.EVENT_LOGIN_LOCKED, event.getEventType());
        assertEquals(SecurityAuditEventEntity.OUTCOME_BLOCKED, event.getOutcome());
        assertEquals("192.168.1.10", event.getIpAddress());
        assertEquals(512, event.getUserAgent().length());
    }

    private UserIdentityEntity createIdentity() {
        UserAccountEntity account = UserAccountEntity.create("Demo User");
        ReflectionTestUtils.setField(account, "id", 1L);
        return UserIdentityEntity.createLocal(account, "demo", "encoded");
    }
}
