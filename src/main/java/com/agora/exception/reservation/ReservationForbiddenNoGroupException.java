package com.agora.exception.reservation;

import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;

public class ReservationForbiddenNoGroupException extends BusinessException {
    public ReservationForbiddenNoGroupException(String message) {
        super(ErrorCode.RESERVATION_FORBIDDEN_NO_GROUP, message);
    }
}
