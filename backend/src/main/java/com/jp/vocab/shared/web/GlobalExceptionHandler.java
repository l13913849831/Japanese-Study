package com.jp.vocab.shared.web;

import com.jp.vocab.shared.api.ApiError;
import com.jp.vocab.shared.api.ApiFieldError;
import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ApiFieldError> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(ApiResponse.failure(
                new ApiError(ErrorCode.VALIDATION_ERROR.name(), "Request validation failed", details)
        ));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        List<ApiFieldError> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(ApiResponse.failure(
                new ApiError(ErrorCode.VALIDATION_ERROR.name(), "Request binding failed", details)
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ApiFieldError> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ApiFieldError(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();

        return ResponseEntity.badRequest().body(ApiResponse.failure(
                new ApiError(ErrorCode.VALIDATION_ERROR.name(), "Constraint validation failed", details)
        ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BAD_REQUEST, IMPORT_ERROR, TEMPLATE_RENDER_ERROR, EXPORT_ERROR, VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return ResponseEntity.status(status).body(ApiResponse.failure(
                ApiError.of(ex.getErrorCode().name(), ex.getMessage())
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(
                ApiError.of(ErrorCode.CONFLICT.name(), "Database constraint violation")
        ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(
                ApiError.of(ErrorCode.BAD_REQUEST.name(), ex.getMessage())
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(
                ApiError.of(ErrorCode.INTERNAL_ERROR.name(), ex.getMessage())
        ));
    }
}
