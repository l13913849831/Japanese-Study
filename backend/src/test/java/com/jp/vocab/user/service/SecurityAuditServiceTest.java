package com.jp.vocab.user.service;

import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.user.dto.SecurityAuditEventResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldListAuditEventsWithNormalizedFilters() {
        SecurityAuditEventEntity event = SecurityAuditEventEntity.create(
                SecurityAuditEventEntity.EVENT_LOGIN_FAILURE,
                SecurityAuditEventEntity.OUTCOME_FAILURE,
                1L,
                "demo",
                "127.0.0.1",
                "JUnit",
                "Invalid username or password"
        );
        when(securityAuditEventRepository.searchAdminAuditEvents(
                eq(SecurityAuditEventEntity.EVENT_LOGIN_FAILURE),
                eq(SecurityAuditEventEntity.OUTCOME_FAILURE),
                eq("%demo%"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event)));

        PageResponse<SecurityAuditEventResponse> response = securityAuditService.listEvents(
                1,
                20,
                "login_failure",
                "failure",
                "demo"
        );

        assertEquals(1, response.items().size());
        assertEquals(SecurityAuditEventEntity.EVENT_LOGIN_FAILURE, response.items().get(0).eventType());
        assertEquals(SecurityAuditEventEntity.OUTCOME_FAILURE, response.items().get(0).outcome());
    }

    private UserIdentityEntity createIdentity() {
        UserAccountEntity account = UserAccountEntity.create("Demo User");
        ReflectionTestUtils.setField(account, "id", 1L);
        return UserIdentityEntity.createLocal(account, "demo", "encoded");
    }
}
