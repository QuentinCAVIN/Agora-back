package com.agora.exception;

import org.springframework.http.HttpStatus;

/**
 * ================================================
 * Enum contractuel regroupant tous les codes d’erreur stables de l’application.
 * ================================================
 */
public enum ErrorCode {

    // ======================================================
    // AUTHENTIFICATION / SÉCURITÉ
    // ======================================================
    AUTH_BAD_CREDENTIALS("AUTH-001", HttpStatus.UNAUTHORIZED, "Identifiants incorrects"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide"),
    AUTH_ACCOUNT_NOT_ALLOWED("AUTH_ACCOUNT_NOT_ALLOWED", HttpStatus.FORBIDDEN, "Ce compte n'est pas autorisé à se connecter"),
    AUTH_REQUIRED("AUTH-002", HttpStatus.UNAUTHORIZED, "Authentification requise ou invalide"),
    ACCESS_DENIED("AUTH-003", HttpStatus.FORBIDDEN, "Accès refusé"),
    AUTH_INVALID_TOKEN("AUTH-004", HttpStatus.UNAUTHORIZED, "Token JWT invalide"),
    AUTH_TOKEN_EXPIRED("AUTH-005", HttpStatus.UNAUTHORIZED, "Token JWT expiré"),
    AUTH_RATE_LIMIT("AUTH-006", HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives. Réessayez plus tard."),
    REFRESH_TOKEN_INVALID("AUTH-007", HttpStatus.UNAUTHORIZED, "Refresh token invalide"),
    REFRESH_TOKEN_EXPIRED("AUTH-008", HttpStatus.UNAUTHORIZED, "Refresh token expiré. Veuillez vous reconnecter."),
    REFRESH_TOKEN_REVOKED("AUTH-009", HttpStatus.UNAUTHORIZED, "Refresh token révoqué. Veuillez vous reconnecter."),
    REFRESH_TOKEN_REUSED("AUTH-010", HttpStatus.UNAUTHORIZED, "Session compromise détectée. Veuillez vous reconnecter."),

    // ======================================================
    // MÉTIER / RÉSERVATION / FICHIERS
    // ======================================================
    SLOT_UNAVAILABLE("RES-001", HttpStatus.CONFLICT, "Créneau déjà occupé au moment de la validation"),
    INVALID_STATUS_TRANSITION("RES-002", HttpStatus.CONFLICT, "Transition de statut non autorisée (paiement, réservation)"),
    LAST_ADMIN_CONSTRAINT("AUTH-011", HttpStatus.CONFLICT, "Impossible de révoquer le dernier SECRETARY_ADMIN"),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT, "Email déjà utilisé lors de l'inscription"),

    DOCUMENT_REQUIRED("RES-003", HttpStatus.UNPROCESSABLE_ENTITY, "Pièce justificative obligatoire manquante pour finaliser la réservation"),
    QUOTA_EXCEEDED("RES-004", HttpStatus.UNPROCESSABLE_ENTITY, "Quota de réservations dépassé"),
    BLACKOUT_PERIOD("RES-005", HttpStatus.UNPROCESSABLE_ENTITY, "Ressource indisponible sur cette période (fermeture)"),

    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Données de requête invalides"),
    DATA_CONFLICT("DATA_CONFLICT", HttpStatus.CONFLICT, "Conflit d'intégrité de données"),

    INVALID_MIME_TYPE("GEN-001", HttpStatus.BAD_REQUEST, "Type de fichier non accepté (détecté par Tika)"),
    FILE_TOO_LARGE("GEN-002", HttpStatus.PAYLOAD_TOO_LARGE, "Fichier dépasse la taille maximale autorisée (5 Mo)"),

    IMPERSONATION_FORBIDDEN("AUTH-013", HttpStatus.FORBIDDEN, "Le compte cible n'est pas de type TUTORED"),

    RESOURCE_NOT_FOUND("GEN-003", HttpStatus.NOT_FOUND, "Entité introuvable (réservation, utilisateur, ressource, etc.)"),
    RESOURCE_TAG_INVALID("GEN-004", HttpStatus.BAD_REQUEST, "Tag d'accessibilité invalide"),
    RESOURCE_CAPACITY_VIOLATION("GEN-005", HttpStatus.BAD_REQUEST, "La capacite est requise pour une ressource IMMOBILIER"),

    ACTIVATION_TOKEN_EXPIRED("AUTH-014", HttpStatus.BAD_REQUEST, "Lien d'activation expiré (> 72h)"),
    ACTIVATION_TOKEN_USED("AUTH-015", HttpStatus.BAD_REQUEST, "Lien d'activation déjà utilisé"),

    // ======================================================
    // EXTERNE / TECHNIQUE
    // ======================================================
    API_UNAVAILABLE("API-001", HttpStatus.INTERNAL_SERVER_ERROR, "API indisponible");

    // ======================================================
    // Champs
    // ======================================================
    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }
    public String defaultMessage() { return defaultMessage; }
}