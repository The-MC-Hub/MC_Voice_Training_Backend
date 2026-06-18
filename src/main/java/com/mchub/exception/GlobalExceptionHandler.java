package com.mchub.exception;

import com.mchub.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.NoHandlerFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(AppException.class)
        public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
                ErrorCode code = ex.getErrorCode();
                log.error("🔴 [{}] {} | HTTP {}", code.getCode(), ex.getMessage(), code.getHttpStatus().value());
                if (ex.getCause() != null) {
                        log.error("   └─ Cause: {}", ex.getCause().getMessage());
                }
                return ResponseEntity
                                .status(Objects.requireNonNull(code.getHttpStatus()))
                                .body(buildErrorResponse(code.getCode(), ex.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
                String details = ex.getBindingResult().getFieldErrors()
                                .stream()
                                .map(FieldError::getDefaultMessage)
                                .collect(Collectors.joining("; "));

                log.warn("🟡 [{}] Validation failed: {}", ErrorCode.VALIDATION_FAILED.getCode(), details);
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(buildErrorResponse(ErrorCode.VALIDATION_FAILED.getCode(), details));
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
                Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
                String details = violations.stream()
                                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                                .collect(Collectors.joining("; "));

                log.warn("🟡 [{}] Constraint violation: {}", ErrorCode.VALIDATION_FAILED.getCode(), details);
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(buildErrorResponse(ErrorCode.VALIDATION_FAILED.getCode(), details));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
                log.warn("🟠 [{}] Access denied: {}", ErrorCode.ACCESS_DENIED.getCode(), ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(buildErrorResponse(ErrorCode.ACCESS_DENIED.getCode(),
                                                ErrorCode.ACCESS_DENIED.getDefaultMessage()));
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
                log.warn("🟠 [{}] Authentication failed: {}", ErrorCode.USER_NOT_AUTHENTICATED.getCode(),
                                ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(buildErrorResponse(ErrorCode.USER_NOT_AUTHENTICATED.getCode(),
                                                ErrorCode.USER_NOT_AUTHENTICATED.getDefaultMessage()));
        }

        @ExceptionHandler(NoHandlerFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException ex) {
                log.warn("🔵 [{}] Route not found: {} {}", ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                                ex.getHttpMethod(), ex.getRequestURL());
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(buildErrorResponse(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                                                "Route does not exist: " + ex.getRequestURL()));
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
                log.warn("🔵 [{}] Method not allowed: {}", ErrorCode.METHOD_NOT_ALLOWED.getCode(), ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(buildErrorResponse(ErrorCode.METHOD_NOT_ALLOWED.getCode(), ex.getMessage()));
        }

        @ExceptionHandler(AsyncRequestNotUsableException.class)
        public void handleClientAbort(AsyncRequestNotUsableException ex) {
                // Client closed connection before response was sent — not a server error, ignore silently
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
                // Suppress client-abort noise at ERROR level
                if (ex.getMessage() != null && ex.getMessage().contains("An established connection was aborted")) {
                        log.debug("Client aborted connection: {}", ex.getMessage());
                        return null;
                }
                log.error("💥 [{}] Unexpected internal error: {}", ErrorCode.INTERNAL_ERROR.getCode(), ex.getMessage(),
                                ex);
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(buildErrorResponse(ErrorCode.INTERNAL_ERROR.getCode(),
                                                ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
        }

        private ApiResponse<Void> buildErrorResponse(String code, String message) {
                ApiResponse<Void> response = new ApiResponse<>("error", message, null);
                response.setSuccess(false);
                response.setErrorCode(code);
                return response;
        }
}
