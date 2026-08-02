package com.libo.mall.order.event;

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
