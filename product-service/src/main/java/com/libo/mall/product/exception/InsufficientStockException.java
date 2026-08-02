package com.libo.mall.product.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, Integer requestedQuantity, Integer availableStock) {
        super("Insufficient stock for product id: " + productId
                + ". Requested: " + requestedQuantity
                + ", available: " + availableStock);
    }
}
