package com.agora.config;

import com.agora.exception.auth.AuthRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    private final SecurityUtils securityUtils = new SecurityUtils();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthenticatedEmail_fromSecurityContext_returnsTrimmedEmail() {
        var auth = new UsernamePasswordAuthenticationToken(
                "  user@example.com  ",
                "n/a",
                AuthorityUtils.NO_AUTHORITIES
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(securityUtils.getAuthenticatedEmail()).isEqualTo("user@example.com");
    }

    @Test
    void getAuthenticatedEmail_whenAnonymous_throws() {
        var anonymous = new AnonymousAuthenticationToken(
                "key",
                "anon",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThatThrownBy(() -> securityUtils.getAuthenticatedEmail())
                .isInstanceOf(AuthRequiredException.class);
    }

    @Test
    void getAuthenticatedEmail_whenNullContext_throws() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> securityUtils.getAuthenticatedEmail())
                .isInstanceOf(AuthRequiredException.class);
    }

    @Test
    void getAuthenticatedEmail_withAuthentication_usesSameRules() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "x@y.fr",
                "pw",
                List.of()
        );

        assertThat(securityUtils.getAuthenticatedEmail(auth)).isEqualTo("x@y.fr");
    }

    @Test
    void getAuthenticatedEmail_blankName_throws() {
        var auth = new UsernamePasswordAuthenticationToken(
                "   ",
                "n/a",
                List.of()
        );

        assertThatThrownBy(() -> securityUtils.getAuthenticatedEmail(auth))
                .isInstanceOf(AuthRequiredException.class);
    }

    @Test
    void tryGetAuthenticatedEmail_whenAnonymous_returnsEmpty() {
        var anonymous = new AnonymousAuthenticationToken(
                "key",
                "anon",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );

        assertThat(securityUtils.tryGetAuthenticatedEmail(anonymous)).isEmpty();
    }

    @Test
    void hasAuthority_detectsRoleOnAuthentication() {
        var auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_SECRETARY_ADMIN"))
        );

        assertThat(securityUtils.hasAuthority(auth, "ROLE_SECRETARY_ADMIN")).isTrue();
        assertThat(securityUtils.hasAuthority(auth, "ROLE_USER")).isFalse();
    }

    @Test
    void hasAuthority_nullAuthentication_isFalse() {
        assertThat(securityUtils.hasAuthority((Authentication) null, "ROLE_SECRETARY_ADMIN")).isFalse();
    }
}
