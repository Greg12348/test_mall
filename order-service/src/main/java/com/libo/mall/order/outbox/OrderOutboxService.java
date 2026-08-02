package com.libo.mall.order.outbox;

import com.libo.mall.order.entity.Order;
import com.libo.mall.order.event.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final String orderCreatedTopic;

    public OrderOutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            @Value("${mall.kafka.topics.order-created}") String orderCreatedTopic
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.orderCreatedTopic = orderCreatedTopic;
    }

    public void saveOrderCreated(Order order) {
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                Instant.now(),
                order.getId(),
                order.getProductId(),
                order.getQuantity()
        );

        try {
            LocalDateTime now = LocalDateTime.now();
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(eventId.toString())
                    .eventType("order-created.v1")
                    .topic(orderCreatedTopic)
                    .messageKey(order.getId().toString())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .nextAttemptAt(now)
                    .createdAt(now)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize order-created event", exception);
        }
    }
}
