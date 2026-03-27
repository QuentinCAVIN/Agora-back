package com.agora.exception.auth;

import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;

public class AuthRequiredException extends BusinessException {

    public AuthRequiredException() {
        super(ErrorCode.AUTH_REQUIRED);
    }
}
