package com.libo.mall.product.dto;

import java.math.BigDecimal;

public record CreateProductRequest(
        String name,
        String description,
        BigDecimal price,
        Integer stock
) {
}
