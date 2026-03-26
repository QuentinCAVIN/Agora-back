package com.agora.exception.auth;

import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;

public class AuthUserNotFoundException extends BusinessException {

    public AuthUserNotFoundException(String email) {
        super(ErrorCode.AUTH_USER_NOT_FOUND, "Utilisateur authentifie introuvable: " + email);
    }
}
