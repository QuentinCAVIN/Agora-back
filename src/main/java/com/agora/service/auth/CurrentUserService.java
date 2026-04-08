package com.agora.service.auth;

import com.agora.exception.auth.AuthRequiredException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    /**
     * Point d'entrée pour les services : évite de propager {@link Authentication} partout.
     */
    public String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return getAuthenticatedEmail(authentication);
    }

    /**
     * Utilisé par les contrôleurs / tests lorsque Spring injecte déjà l'{@link Authentication}.
     */
    public String getAuthenticatedEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthRequiredException();
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new AuthRequiredException();
        }

        return email.trim();
    }
}
