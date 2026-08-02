package com.libo.mall.product.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        Long orderId,
        Long productId,
        Integer quantity
) {
}
