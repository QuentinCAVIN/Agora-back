package com.agora.service.auth;

import com.agora.entity.user.ERole;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void generateAccessToken_adminEmail_shouldContainSecretaryAdminAndSuperadminRoles() {
        JwtService jwtService = new JwtService(SECRET, 3600, 7200, "admin@agora.local");
        User user = buildUser("admin@agora.local");

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractRoles(token))
                .containsExactlyInAnyOrder("ROLE_SECRETARY_ADMIN", "ROLE_SUPERADMIN");
    }

    @Test
    void generateAccessToken_nonAdminEmail_shouldContainNoRoles() {
        JwtService jwtService = new JwtService(SECRET, 3600, 7200, "admin@agora.local");
        User user = buildUser("user@agora.local");

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractRoles(token)).isEqualTo(List.of());
    }

    @Test
    void generateAccessToken_secretaryAdminRole_shouldContainSecretaryAdminRole() {
        JwtService jwtService = new JwtService(SECRET, 3600, 7200, "admin@agora.local");
        User user = buildUser("secretary@agora.local");
        user.addRole(ERole.SECRETARY_ADMIN);

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractRoles(token))
                .containsExactly("ROLE_SECRETARY_ADMIN");
    }

    @Test
    void generateAccessToken_delegateAdminRole_shouldContainSecretaryAdminRole() {
        JwtService jwtService = new JwtService(SECRET, 3600, 7200, "admin@agora.local");
        User user = buildUser("delegate@agora.local");
        user.addRole(ERole.DELEGATE_ADMIN);

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractRoles(token))
                .containsExactly("ROLE_SECRETARY_ADMIN");
    }

    private static User buildUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);
        return user;
    }
}
