package com.agora.exception.auth;

import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;

public class EmailAlreadyExistsException extends BusinessException {

    public EmailAlreadyExistsException(String email) {
        super(ErrorCode.EMAIL_ALREADY_EXISTS, "Un compte avec cet email existe déjà : " + email);
    }
}
