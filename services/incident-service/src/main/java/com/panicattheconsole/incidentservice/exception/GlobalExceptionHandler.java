package com.panicattheconsole.incidentservice.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for consistent error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        log.warn("Resource not found: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "NOT_FOUND");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidState(IllegalStateException e) {
        log.warn("Invalid state: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "CONFLICT");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        Map<String, String> error = new HashMap<>();
        error.put("error", "INTERNAL_SERVER_ERROR");
        error.put("message", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
