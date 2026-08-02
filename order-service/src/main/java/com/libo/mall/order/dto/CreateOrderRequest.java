package com.libo.mall.order.dto;

import java.math.BigDecimal;

public record CreateOrderRequest(
        Long userId,
        Long productId,
        Integer quantity,
        BigDecimal totalAmount
) {
}
