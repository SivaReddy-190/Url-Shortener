package com.project.urlshortener.exception;

import com.project.urlshortener.dto.url.UrlErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<UrlErrorResponseDto> handleNotFound(UrlNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new UrlErrorResponseDto("404", ex.getMessage()));
    }

    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<UrlErrorResponseDto> handleExpired(UrlExpiredException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new UrlErrorResponseDto("410", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<UrlErrorResponseDto> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new UrlErrorResponseDto("400", ex.getMessage()));
    }

    @ExceptionHandler(ShortLinkCollisionException.class)
    public ResponseEntity<UrlErrorResponseDto> handleCollision(ShortLinkCollisionException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UrlErrorResponseDto("500", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UrlErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(new UrlErrorResponseDto("400", message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<UrlErrorResponseDto> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new UrlErrorResponseDto("403", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<UrlErrorResponseDto> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new UrlErrorResponseDto("401", "Invalid username or password"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UrlErrorResponseDto> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UrlErrorResponseDto("500", "An unexpected error occurred"));
    }
}
