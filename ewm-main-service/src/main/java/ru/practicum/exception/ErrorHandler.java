package ru.practicum.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    private static final String REASON_BAD_REQUEST = "Incorrectly made request.";
    private static final String REASON_NOT_FOUND = "The required object was not found.";
    private static final String REASON_CONFLICT = "For the requested operation the conditions are not met.";
    private static final String REASON_INTEGRITY = "Integrity constraint has been violated.";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException e) {
        log.warn("Not found: {}", e.getMessage());
        return build(HttpStatus.NOT_FOUND, REASON_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException e) {
        log.warn("Validation error: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, REASON_BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException e) {
        log.warn("Conflict: {}", e.getMessage());
        return build(HttpStatus.CONFLICT, REASON_CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        return build(HttpStatus.CONFLICT, REASON_INTEGRITY, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        return build(HttpStatus.BAD_REQUEST, REASON_BAD_REQUEST, message);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception e) {
        log.warn("Bad request: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, REASON_BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiError> handleUnexpected(Throwable e) {
        log.error("Unexpected error", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred.", e.getMessage());
    }

    private String formatFieldError(FieldError error) {
        return "Field: " + error.getField() + ". Error: " + error.getDefaultMessage()
                + ". Value: " + error.getRejectedValue();
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String reason, String message) {
        ApiError apiError = ApiError.builder()
                .status(status.name())
                .reason(reason)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(apiError);
    }
}
