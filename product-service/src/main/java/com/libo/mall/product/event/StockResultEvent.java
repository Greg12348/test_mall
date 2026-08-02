package com.libo.mall.product.event;

import java.time.Instant;
import java.util.UUID;

public record StockResultEvent(
        UUID eventId,
        UUID causationId,
        Instant occurredAt,
        Long orderId,
        boolean reserved,
        String reason
) {
}
