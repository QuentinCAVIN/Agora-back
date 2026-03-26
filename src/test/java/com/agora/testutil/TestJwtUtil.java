package com.agora.testutil;


import java.util.List;

public class TestJwtUtil {

    /**
     * Utilitaire de tests "plug & play" (ne dépend pas du JWT réel).
     * Tant que le filtre JWT n'est pas branché, les tests peuvent utiliser ces valeurs
     * comme Authorization header sans validation côté backend.
     */
    public String createToken(String email, List<String> roles, String userId, String internalRef, String phone) {
        // Format stable (lisible dans les logs), sans signature ni crypto.
        String rolesPart = (roles == null || roles.isEmpty()) ? "none" : String.join(",", roles);
        return "test-token(email=" + email + ";roles=" + rolesPart + ";uid=" + userId + ")";
    }

    public String createAdminToken(String email, String userId) {
        return createToken(email, List.of("ROLE_ADMIN"), userId, null, null);
    }

    public String createUserToken(String email, String userId) {
        return createToken(email, List.of("ROLE_USER"), userId, null, null);
    }
}