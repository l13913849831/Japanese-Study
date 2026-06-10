package com.jp.vocab.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.vocab.shared.auth.MobileTokenAuthenticationFilter;
import com.jp.vocab.shared.auth.RestAccessDeniedHandler;
import com.jp.vocab.shared.auth.RestAuthenticationEntryPoint;
import com.jp.vocab.shared.auth.SecurityConfig;
import com.jp.vocab.shared.auth.UserRole;
import com.jp.vocab.shared.config.CorsProperties;
import com.jp.vocab.shared.config.MobileSessionProperties;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.LogoutResponse;
import com.jp.vocab.user.dto.MobileAuthSessionResponse;
import com.jp.vocab.user.service.MobileAuthService;
import com.jp.vocab.user.service.MobileSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileAuthController.class)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        MobileTokenAuthenticationFilter.class
})
@EnableConfigurationProperties({CorsProperties.class, MobileSessionProperties.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class MobileAuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MobileAuthService mobileAuthService;

    @MockBean
    private MobileSessionService mobileSessionService;

    @Test
    void shouldRejectMobileMeWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/mobile/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verify(mobileAuthService, never()).getCurrentMobileUser();
    }

    @Test
    void shouldAllowMobileMeWithBearerToken() throws Exception {
        when(mobileSessionService.authenticate("mobile-token"))
                .thenReturn(Optional.of(new com.jp.vocab.shared.auth.AppUserPrincipal(7L, "wechat-miniapp:7", "Mobile Learner", "", UserRole.USER, true)));
        when(mobileAuthService.getCurrentMobileUser())
                .thenReturn(new CurrentUserResponse(7L, "wechat-miniapp:7", "Mobile Learner", "WORD_FIRST", List.of(UserRole.USER)));

        mockMvc.perform(get("/api/mobile/me")
                        .header("Authorization", "Bearer mobile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("wechat-miniapp:7"));

        verify(mobileAuthService).getCurrentMobileUser();
    }

    @Test
    void shouldAllowWechatLoginWithoutCsrfToken() throws Exception {
        when(mobileAuthService.login(any(), any()))
                .thenReturn(new MobileAuthSessionResponse(
                        "mobile-token",
                        OffsetDateTime.parse("2026-06-10T12:00:00+09:00"),
                        new CurrentUserResponse(7L, "wechat-miniapp:7", "Mobile Learner", "WORD_FIRST", List.of(UserRole.USER))
                ));

        mockMvc.perform(post("/api/mobile/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "wx-code"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mobile-token"));

        verify(mobileAuthService).login(any(), any());
    }

    @Test
    void shouldAllowLogoutWithoutCsrfTokenWhenBearerTokenIsPresent() throws Exception {
        when(mobileSessionService.authenticate("mobile-token"))
                .thenReturn(Optional.of(new com.jp.vocab.shared.auth.AppUserPrincipal(7L, "wechat-miniapp:7", "Mobile Learner", "", UserRole.USER, true)));
        when(mobileAuthService.logout(any())).thenReturn(new LogoutResponse(true));

        mockMvc.perform(post("/api/mobile/auth/logout")
                        .header("Authorization", "Bearer mobile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loggedOut").value(true));

        verify(mobileAuthService).logout(any());
    }
}
