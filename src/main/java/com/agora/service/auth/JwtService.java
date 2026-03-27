package com.agora.service.auth;

import com.agora.entity.user.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiresInSeconds;
    private final long refreshExpiresInSeconds;
    private final String adminEmail;

    public JwtService(
            @Value("${agora.jwt.secret}") String secret,
            @Value("${agora.jwt.expires-in-seconds}") long expiresInSeconds,
            @Value("${agora.jwt.refresh-expires-in-seconds}") long refreshExpiresInSeconds,
            @Value("${agora.auth.admin-email:admin@agora.local}") String adminEmail
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("La configuration agora.jwt.secret est manquante");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("La configuration agora.jwt.secret doit faire au moins 32 caractères");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiresInSeconds = expiresInSeconds;
        this.refreshExpiresInSeconds = refreshExpiresInSeconds;
        this.adminEmail = (adminEmail == null) ? "admin@agora.local" : adminEmail.trim();
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public long getRefreshExpiresInSeconds() {
        return refreshExpiresInSeconds;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expiresInSeconds);

        List<String> roles = (user.getEmail() != null && user.getEmail().equalsIgnoreCase(adminEmail))
                ? List.of("ROLE_SECRETARY_ADMIN")
                : List.of();

        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("email", user.getEmail())
                .claim("accountType", user.getAccountType().name())
                .claim("accountStatus", user.getAccountStatus().name())
                .claim("roles", roles)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshExpiresInSeconds);

        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("typ", "refresh")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        Claims claims = parseClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return Collections.emptyList();
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            Object typ = claims.get("typ");
            return "refresh".equals(typ);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
