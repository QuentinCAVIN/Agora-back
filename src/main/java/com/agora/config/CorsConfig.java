package com.agora.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS pour appels navigateur (front SPA, autres ports, HTTPS local, LAN, devices sur Wi‑Fi, etc.).
 * Les défauts couvrent le dev courant ; en production, restreindre explicitement via {@code agora.cors.allowed-origin-patterns}.
 */
@Configuration
@EnableConfigurationProperties(AgoraCorsProperties.class)
@RequiredArgsConstructor
public class CorsConfig {

    private static final List<String> DEFAULT_ORIGIN_PATTERNS = List.of("*");

    private static List<String> buildDefaultOriginPatterns() {
        List<String> patterns = new ArrayList<>(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://[::1]:*",
                "https://localhost:*",
                "https://127.0.0.1:*",
                "https://[::1]:*",
                "http://*.local:*",
                "https://*.local:*",
                "http://10.*.*.*:*",
                "https://10.*.*.*:*",
                "http://192.168.*.*:*",
                "https://192.168.*.*:*"
        ));
        for (int octet2 = 16; octet2 <= 31; octet2++) {
            patterns.add("http://172." + octet2 + ".*.*:*");
            patterns.add("https://172." + octet2 + ".*.*:*");
        }
        return List.copyOf(patterns);
    }

    private static final List<String> DEFAULT_ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
    );

    private static final List<String> DEFAULT_ALLOWED_HEADERS = List.of("*");

    /**
     * En-têtes que le navigateur peut lire côté JS (hors liste « simple » CORS).
     */
    private static final List<String> DEFAULT_EXPOSED_HEADERS = List.of(
            "Authorization",
            "Content-Disposition",
            "Location",
            "Set-Cookie"
    );

    private final AgoraCorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(
                firstNonEmpty(corsProperties.getAllowedOriginPatterns(), DEFAULT_ORIGIN_PATTERNS)
        );
        config.setAllowedMethods(
                firstNonEmpty(corsProperties.getAllowedMethods(), DEFAULT_ALLOWED_METHODS)
        );
        config.setAllowedHeaders(
                firstNonEmpty(corsProperties.getAllowedHeaders(), DEFAULT_ALLOWED_HEADERS)
        );
        config.setExposedHeaders(
                firstNonEmpty(corsProperties.getExposedHeaders(), DEFAULT_EXPOSED_HEADERS)
        );
        config.setAllowCredentials(corsProperties.isAllowCredentials());
        config.setMaxAge(corsProperties.getMaxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // S'assurer que la configuration s'applique à tous les chemins, y compris l'auth
        source.registerCorsConfiguration("/**", config);
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static List<String> firstNonEmpty(List<String> configured, List<String> defaults) {
        if (CollectionUtils.isEmpty(configured)) {
            return defaults;
        }
        return List.copyOf(configured);
    }
}
