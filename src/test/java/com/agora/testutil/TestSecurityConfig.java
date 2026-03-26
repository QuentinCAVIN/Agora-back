package com.agora.testutil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

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
    public SecurityFilterChain testSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .build();
    }
}