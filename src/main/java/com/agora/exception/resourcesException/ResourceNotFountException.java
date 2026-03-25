package com.agora.exception.resourcesException;

import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;

public class ResourceNotFountException extends BusinessException {
    public ResourceNotFountException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND,message);
    }
}
