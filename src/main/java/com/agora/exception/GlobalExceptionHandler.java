package com.agora.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ======================================================
    //  CORE BUILDER
    // ======================================================
    private ResponseEntity<ApiError> buildError(
            ErrorCode code,
            String message,
            HttpServletRequest request,
            Exception ex) {

        String finalMessage = (message != null && !message.isBlank())
                ? message
                : code.defaultMessage();
        String traceId = MDC.get("traceId");
        String correlationId = MDC.get("correlationId");

        ApiError error = new ApiError(code, message, request.getRequestURI(), traceId, correlationId);

        if (code.status().is5xxServerError()) {
            log.error("[{}] {} -> {}", code.code(), request.getRequestURI(), finalMessage, ex);
        } else if (code.status().is4xxClientError()) {
            log.warn("[{}] {} -> {}", code.code(), request.getRequestURI(), finalMessage);
        } else {
            log.info("[{}] {} -> {}", code.code(), request.getRequestURI(), finalMessage);
        }

        return ResponseEntity.status(code.status()).body(error);
    }

    private ResponseEntity<ApiError> buildError(
            ErrorCode code,
            String message,
            HttpServletRequest request) {
        return buildError(code, message, request, null);
    }

    // ======================================================
    //  BUSINESS — couvre EmailAlreadyExists, AuthInvalidCredentials,
    //             AuthAccountNotAllowed, ResourceNotFound, etc.
    // ======================================================
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(
            BusinessException ex,
            HttpServletRequest request) {
        return buildError(ex.getCode(), ex.getMessage(), request, ex);
    }

    // ======================================================
    //  VALIDATION (@Valid sur les DTOs)
    // ======================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildError(ErrorCode.VALIDATION_ERROR, message, request, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String message = ex.getConstraintViolations().iterator().next().getMessage();
        return buildError(ErrorCode.VALIDATION_ERROR, message, request, ex);
    }

    // ======================================================
    //  DATABASE — conflit d'intégrité générique
    // ======================================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildError(ErrorCode.DATA_CONFLICT, null, request, ex);
    }

    // ======================================================
    //  FALLBACK
    //  TODO: Étape sécurité — ajouter handler AccessDeniedException et AuthenticationException
    // ======================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        return buildError(ErrorCode.API_UNAVAILABLE, null, request, ex);
    }
}
