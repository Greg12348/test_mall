package com.libo.mall.order.exception;

import com.libo.mall.order.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(
            OrderNotFoundException exception
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(status.value())
                .message(exception.getMessage())
                .data(null)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
