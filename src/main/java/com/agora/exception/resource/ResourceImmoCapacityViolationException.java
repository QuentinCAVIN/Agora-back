package com.agora.exception.resource;

import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;

public class ResourceImmoCapacityViolationException extends BusinessException {
    public ResourceImmoCapacityViolationException(String message) {
        super(ErrorCode.RESOURCE_CAPACITY_VIOLATION, message);
    }
}
