package com.agora.integration;

import com.agora.entity.group.Group;
import com.agora.entity.group.GroupMembership;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "security-real-it"})
@Tag("integration-auth-refresh-cookie")
class AuthRefreshCookieIntegrationTest {

    private static final String PUBLIC_GROUP_NAME = "Public";
    private static final String EMAIL = "refresh.cookie.it@agora.local";
    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupMembershipRepository groupMembershipRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpUserAndPublicGroup() {
        Group publicGroup = groupRepository.findByName(PUBLIC_GROUP_NAME).orElseGet(() -> {
            Group g = new Group();
            g.setName(PUBLIC_GROUP_NAME);
            g.setPreset(true);
            return groupRepository.save(g);
        });

        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(EMAIL);
            u.setPasswordHash(passwordEncoder.encode(PASSWORD));
            u.setFirstName("Refresh");
            u.setLastName("Cookie");
            u.setPhone("0600000011");
            u.setAccountType(AccountType.AUTONOMOUS);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return userRepository.save(u);
        });

        if (!groupMembershipRepository.existsByUserIdAndGroupId(user.getId(), publicGroup.getId())) {
            GroupMembership membership = new GroupMembership();
            membership.setUser(user);
            membership.setGroup(publicGroup);
            membership.setJoinedAt(Instant.now());
            groupMembershipRepository.save(membership);
        }
    }

    @Test
    void login_shouldSetHttpOnlyRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "refresh.cookie.it@agora.local",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void refresh_shouldWorkWithCookie() throws Exception {
        String setCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "refresh.cookie.it@agora.local",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        // Ne garder que la partie "name=value" (sinon WebUtils.getCookie ne matche pas)
        String cookiePair = setCookie.split(";", 2)[0];
        String refreshValue = cookiePair.split("=", 2)[1];

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")));
    }

    @Test
    void logout_shouldClearCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }
}

