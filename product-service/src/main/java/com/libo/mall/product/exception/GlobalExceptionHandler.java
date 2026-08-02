package com.libo.mall.product.exception;

import com.libo.mall.product.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(
            ProductNotFoundException exception
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(status.value())
                .message(exception.getMessage())
                .data(null)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(
            InsufficientStockException exception
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(status.value())
                .message(exception.getMessage())
                .data(null)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
