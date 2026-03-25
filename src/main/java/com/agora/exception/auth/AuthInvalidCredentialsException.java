package com.agora.exception.auth;

import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;

public class AuthInvalidCredentialsException extends BusinessException {

    public AuthInvalidCredentialsException() {
        super(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }
}
