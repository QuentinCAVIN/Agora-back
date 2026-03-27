package com.agora.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Active {@code @PreAuthorize} / {@code @PostAuthorize} hors profil {@code test}.
 * <p>
 * Le profil {@code test} désactive volontairement la sécurité méthode pour les tests
 * d'intégration qui ne simulent pas encore des rôles JWT réels.
 */
@Configuration
@EnableMethodSecurity
@Profile("!test")
public class MethodSecurityConfig {
}
