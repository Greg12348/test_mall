package com.libo.mall.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;
}
