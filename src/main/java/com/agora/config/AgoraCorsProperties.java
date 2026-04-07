package com.agora.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration CORS (surchargeable via {@code agora.cors.*} ou variables d'environnement
 * {@code AGORA_CORS_*}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "agora.cors")
public class AgoraCorsProperties {

    /**
     * Patterns d'origine autorisés (syntaxe Spring {@link org.springframework.web.cors.CorsConfiguration}).
     * Liste vide ou absente = utilisation des valeurs par défaut dans {@link CorsConfig}.
     */
    private List<String> allowedOriginPatterns = new ArrayList<>();

    private List<String> allowedMethods = new ArrayList<>();

    private List<String> allowedHeaders = new ArrayList<>();

    private List<String> exposedHeaders = new ArrayList<>();

    private boolean allowCredentials = true;

    private long maxAgeSeconds = 3600L;
}
