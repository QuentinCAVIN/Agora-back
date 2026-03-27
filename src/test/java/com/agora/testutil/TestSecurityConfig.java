package com.agora.testutil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Profile("test & !security-real-it")
@Configuration
public class TestSecurityConfig {

    /**
     * Configuration volontairement permissive pour les tests metier d'integration.
     *
     * Ne PAS utiliser comme preuve de securite:
     * la validation des acces est couverte par des tests WebMvc dedies.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain testSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // Injecte un user admin pour satisfaire les @PreAuthorize en tests d'intégration
                .addFilterBefore(testAdminAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public OncePerRequestFilter testAdminAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain
            ) throws ServletException, IOException {
                var auth = new UsernamePasswordAuthenticationToken(
                        "test-admin",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_SECRETARY_ADMIN"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }
        };
    }
}