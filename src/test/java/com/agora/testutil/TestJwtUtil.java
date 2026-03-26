package com.agora.testutil;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class TestJwtUtil {

    private final SecretKey key;
    private final long expiresInSeconds;

    public TestJwtUtil(
            @Value("${agora.jwt.secret}") String secret,
            @Value("${agora.jwt.expires-in-seconds}") long expiresInSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiresInSeconds = expiresInSeconds;
    }

    /**
     * Génère un JWT de test signé, compatible avec JwtAuthenticationFilter.
     *
     * Contrat MVP:
     * - sub = email
     * - signature HS256
     * - expiration gérée
     */
    public String createToken(String email, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expiresInSeconds);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("roles", roles == null ? List.of() : roles)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createValidUserToken(String email) {
        return createToken(email, List.of());
    }

    public String createExpiredUserToken(String email) {
        Instant now = Instant.now();
        Instant exp = now.minusSeconds(60);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(Date.from(now.minusSeconds(120)))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}