package com.warehouse.system.Exception;

import com.warehouse.system.DTO.Response.ApiError;
import com.warehouse.system.DTO.Response.UserResponse;
import com.warehouse.system.Enums.AuthAction;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExist.class)
    public ResponseEntity<ApiError> handleEmailExists(EmailAlreadyExist ex) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "EMAIL_ALREADY_EXISTS",
                ex.getMessage(),
                Map.of("action", AuthAction.LOGIN_REQUIRED.toString())
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                ex.getMessage(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    @ExceptionHandler(InvalidCredentials.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentials ex) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                ex.getMessage(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ApiError> handleBooking(BookingException ex) {
        ResponseStatus rs = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        HttpStatus status = rs != null ? rs.code() : HttpStatus.BAD_REQUEST;
        var body = new ApiError(Instant.now(), status.value(),
                ex.getClass().getSimpleName().toUpperCase(), ex.getMessage(), null);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (ex instanceof BookingException.Busy) {
            response.header(HttpHeaders.RETRY_AFTER, "1");
        }
        return response.body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(), 400, "VALIDATION", "Invalid request", fields));
    }
}
