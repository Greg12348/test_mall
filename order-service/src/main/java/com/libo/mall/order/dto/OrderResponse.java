package com.libo.mall.order.dto;

import com.libo.mall.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long userId,
        Long productId,
        Integer quantity,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
