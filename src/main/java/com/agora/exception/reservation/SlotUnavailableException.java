package com.agora.exception.reservation;

import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;

public class SlotUnavailableException extends BusinessException {
    public SlotUnavailableException(String message) {
        super(ErrorCode.SLOT_UNAVAILABLE, message);
    }
}
