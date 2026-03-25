package com.agora.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Getter
public class ApiError {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final OffsetDateTime timestamp;

    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;

    private final String traceId;
    private final String correlationId;

    public ApiError(ErrorCode errorCode, String message, String path, String traceId, String correlationId) {
        this.timestamp = OffsetDateTime.now(ZoneOffset.UTC);
        this.status = errorCode.status().value();
        this.error = errorCode.status().name();
        this.code = errorCode.code();
        this.message = (message != null && !message.isBlank())
                ? message
                : errorCode.defaultMessage();
        this.path = path;
        this.traceId = traceId;
        this.correlationId = correlationId;
    }

    public ApiError(HttpStatus status, String message, String path, String traceId, String correlationId) {
        this.timestamp = OffsetDateTime.now(ZoneOffset.UTC);
        this.status = status.value();
        this.error = status.name();
        this.code = null;
        this.message = message;
        this.path = path;
        this.traceId = traceId;
        this.correlationId = correlationId;
    }
}
