package com.agora.exception.auth;

import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;

public class AuthAccountNotAllowedException extends BusinessException {

    public AuthAccountNotAllowedException(String message) {
        super(ErrorCode.AUTH_ACCOUNT_NOT_ALLOWED, message);
    }
}
