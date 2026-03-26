package com.agora.integration;

import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.repository.user.UserRepository;
import com.agora.testutil.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite d'integration "securite reelle MVP" (phase 2).
 *
 * Objectif:
 * - valider public vs prive et auth vs non-auth avec une chaine de securite reelle,
 * - avec JWT signe de test (sub=email).
 *
 * Note:
 * - Cette suite n'utilise pas IntegrationTestBase.
 * - Le profil "test" reste actif pour H2/Flyway.
 * - Le profil "security-real-it" desactive TestSecurityConfig permissif.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "security-real-it"})
@Tag("integration-security-real")
class SecurityRealIntegrationTest {

    private static final String AUTH_ME_URL = "/api/auth/me";
    private static final String RESOURCES_URL = "/api/resources";
    private static final String TEST_EMAIL = "security-it-user@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @BeforeEach
    void setUpUserForAuthMe() {
        if (userRepository.findByEmailIgnoreCase(TEST_EMAIL).isEmpty()) {
            User user = new User();
            user.setEmail(TEST_EMAIL);
            user.setFirstName("Security");
            user.setLastName("IT");
            user.setPhone("0600000000");
            user.setAccountType(AccountType.AUTONOMOUS);
            user.setAccountStatus(AccountStatus.ACTIVE);
            userRepository.save(user);
        }
    }

    @Test
    void getResources_anonymous_shouldSucceed() throws Exception {
        mockMvc.perform(get(RESOURCES_URL))
                .andExpect(status().isOk());
    }

    @Test
    void getResources_withValidToken_shouldStillBePublic() throws Exception {
        String token = testJwtUtil.createValidUserToken(TEST_EMAIL);

        mockMvc.perform(get(RESOURCES_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getMe_withoutToken_shouldFail() throws Exception {
        mockMvc.perform(get(AUTH_ME_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMe_withInvalidToken_shouldFail() throws Exception {
        mockMvc.perform(get(AUTH_ME_URL)
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMe_withValidToken_shouldSucceed() throws Exception {
        String token = testJwtUtil.createValidUserToken(TEST_EMAIL);

        mockMvc.perform(get(AUTH_ME_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
