package com.agora.exception;

import com.agora.exception.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode code;

    public BusinessException(ErrorCode code) {
        super(code.defaultMessage());
        this.code = code;
    }

    public BusinessException(ErrorCode code, String customMessage) {
        super(customMessage);
        this.code = code;
    }


}
