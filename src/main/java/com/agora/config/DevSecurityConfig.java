package com.agora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sécurité DEV uniquement.
 *
 * Objectif: permettre au front de tester l'API tant que le JWT n'est pas branché
 * (pas de filtre d'auth, pas de validation du Bearer).
 *
 * À retirer / durcir quand le ticket JWT sera prêt.
 */
@Profile({"dev", "local", "seed"})
@Configuration
public class DevSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}

