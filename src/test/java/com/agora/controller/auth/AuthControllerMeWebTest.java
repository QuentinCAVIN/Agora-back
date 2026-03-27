package com.agora.controller.auth;

import com.agora.config.SecurityConfig;
import com.agora.dto.response.auth.AuthMeResponseDto;
import com.agora.dto.response.auth.UserGroupSummaryDto;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.service.auth.JwtService;
import com.agora.service.auth.AuthMeService;
import com.agora.service.auth.AuthService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@Tag("security-web")
class AuthControllerMeWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthMeService authMeService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "user@example.com")
    void me_shouldReturnProfileWhenAuthenticated() throws Exception {
        AuthMeResponseDto response = new AuthMeResponseDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "user@example.com",
                "Jane",
                "Doe",
                AccountType.AUTONOMOUS,
                AccountStatus.ACTIVE,
                "0600000000",
                List.of(new UserGroupSummaryDto(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "Public",
                        true
                )),
                Instant.parse("2026-03-26T10:15:30Z")
        );

        when(authMeService.getCurrentUserProfile(any())).thenReturn(response);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.groups[0].name").value("Public"))
                .andExpect(jsonPath("$.groups[0].isPreset").value(true));
    }

    @Test
    void me_shouldBeRejectedWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }
}
