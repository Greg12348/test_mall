package com.libo.mall.order.event;

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
