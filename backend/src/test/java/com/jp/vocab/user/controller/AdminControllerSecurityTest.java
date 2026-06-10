package com.jp.vocab.user.controller;

import com.jp.vocab.shared.auth.RestAccessDeniedHandler;
import com.jp.vocab.shared.auth.RestAuthenticationEntryPoint;
import com.jp.vocab.shared.auth.SecurityConfig;
import com.jp.vocab.shared.config.CorsProperties;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.service.AdminUserService;
import com.jp.vocab.user.service.SecurityAlertService;
import com.jp.vocab.user.service.SecurityAuditService;
import com.jp.vocab.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private SecurityAuditService securityAuditService;

    @MockBean
    private SecurityAlertService securityAlertService;

    @Test
    void shouldRejectUnauthenticatedAdminRequest() throws Exception {
        mockMvc.perform(get("/api/admin/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/admin/me").with(user("demo").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void shouldAllowAdminUser() throws Exception {
        when(userProfileService.getCurrentUserProfile())
                .thenReturn(new CurrentUserResponse(1L, "admin", "Admin User", "WORD_FIRST", List.of("ADMIN")));

        mockMvc.perform(get("/api/admin/me").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
    }
}
