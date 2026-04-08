package com.agora.config;

import com.agora.exception.auth.AuthRequiredException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Point unique pour la lecture du contexte de sécurité Spring (JWT / session).
 * <p>
 * Ne pas dupliquer {@link SecurityContextHolder} ni les contrôles
 * {@link AnonymousAuthenticationToken} dans les services ou contrôleurs.
 */
@Component
public class SecurityUtils {

    /**
     * Email de l'utilisateur authentifié (depuis le {@link SecurityContextHolder}).
     *
     * @throws AuthRequiredException si anonyme ou principal invalide
     */
    public String getAuthenticatedEmail() {
        return getAuthenticatedEmail(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Email lorsque l'{@link Authentication} est déjà fourni (ex. paramètre de contrôleur).
     *
     * @throws AuthRequiredException si non authentifié ou principal invalide
     */
    public String getAuthenticatedEmail(Authentication authentication) {
        if (!isMeaningfullyAuthenticated(authentication)) {
            throw new AuthRequiredException();
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new AuthRequiredException();
        }

        return email.trim();
    }

    /**
     * Email si présent, sinon vide (appels publics, audit, filtres optionnels).
     */
    public Optional<String> tryGetAuthenticatedEmail() {
        return tryGetAuthenticatedEmail(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Email si l'appel est authentifié de façon nominale, sinon vide.
     */
    public Optional<String> tryGetAuthenticatedEmail(Authentication authentication) {
        if (!isMeaningfullyAuthenticated(authentication)) {
            return Optional.empty();
        }
        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(email.trim());
    }

    /**
     * Vérifie une autorité Spring Security sur le contexte courant (ex. {@code ROLE_SECRETARY_ADMIN}).
     */
    public boolean hasAuthority(String authority) {
        return hasAuthority(SecurityContextHolder.getContext().getAuthentication(), authority);
    }

    /**
     * Vérifie une autorité sur une authentification donnée (ex. endpoint public avec auth optionnelle).
     */
    public boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || !authentication.isAuthenticated() || authority == null || authority.isBlank()) {
            return false;
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMeaningfullyAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
