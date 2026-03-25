package com.agora.exception;

public class AuthAccountNotAllowedException extends BusinessException {

    public AuthAccountNotAllowedException(String message) {
        super(ErrorCode.AUTH_ACCOUNT_NOT_ALLOWED, message);
    }
}
