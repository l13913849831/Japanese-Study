package com.jp.vocab.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.vocab.shared.auth.RestAccessDeniedHandler;
import com.jp.vocab.shared.auth.RestAuthenticationEntryPoint;
import com.jp.vocab.shared.auth.SecurityConfig;
import com.jp.vocab.shared.config.CorsProperties;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void shouldExposeCsrfTokenForSpaClients() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.data.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void shouldRejectLoginWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "demo",
                                  "password": "demo123456"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(authService, times(0)).login(any(), any(), any());
    }

    @Test
    void shouldAllowLoginWithCsrfToken() throws Exception {
        when(authService.login(any(), any(), any()))
                .thenReturn(new CurrentUserResponse(1L, "demo", "Demo User", "WORD_FIRST"));

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        String csrfToken = payload.path("data").path("token").asText();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "demo",
                                  "password": "demo123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo"));

        verify(authService).login(any(), any(), any());
    }
}
