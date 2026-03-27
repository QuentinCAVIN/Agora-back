package com.agora.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Centralise la construction du cookie HttpOnly de refresh token.
 * Objectif: compatible front (Angular) avec "withCredentials: true".
 */
@Service
public class AuthCookieService {

    private final String cookieName;
    private final String cookiePath;
    private final String cookieSameSite;
    private final boolean cookieSecure;
    private final String cookieDomain;

    public AuthCookieService(
            @Value("${agora.auth.refresh-cookie.name}") String cookieName,
            @Value("${agora.auth.refresh-cookie.path}") String cookiePath,
            @Value("${agora.auth.refresh-cookie.same-site}") String cookieSameSite,
            @Value("${agora.auth.refresh-cookie.secure}") boolean cookieSecure,
            @Value("${agora.auth.refresh-cookie.domain:}") String cookieDomain
    ) {
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.cookieSameSite = cookieSameSite;
        this.cookieSecure = cookieSecure;
        this.cookieDomain = (cookieDomain == null) ? "" : cookieDomain.trim();
    }

    public String getCookieName() {
        return cookieName;
    }

    public ResponseCookie buildRefreshCookie(String refreshToken, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(cookiePath)
                .sameSite(cookieSameSite)
                .maxAge(maxAgeSeconds);

        if (!cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }

    public ResponseCookie clearRefreshCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(cookiePath)
                .sameSite(cookieSameSite)
                .maxAge(0);

        if (!cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }
}

