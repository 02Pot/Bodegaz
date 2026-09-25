package com.warehouse.system.Exception;

import com.warehouse.system.DTO.Response.UserResponse;
import com.warehouse.system.Enums.AuthAction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyExist.class)
    public ResponseEntity<UserResponse> handleEmailExists(EmailAlreadyExist ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new UserResponse(false, ex.getMessage(), AuthAction.LOGIN_REQUIRED));
    }
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", ex.getMessage()));
    }
    @ExceptionHandler(InvalidCredentials.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentials ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", ex.getMessage()));
    }
}
