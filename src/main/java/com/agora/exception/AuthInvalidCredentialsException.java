package com.agora.exception;

public class AuthInvalidCredentialsException extends BusinessException {

    public AuthInvalidCredentialsException() {
        super(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }
}
