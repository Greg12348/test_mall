package com.libo.mall.product.outbox;

import com.libo.mall.product.event.StockResultEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
public class ProductOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final String stockResultTopic;

    public ProductOutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            @Value("${mall.kafka.topics.stock-result}") String stockResultTopic
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.stockResultTopic = stockResultTopic;
    }

    public void saveStockResult(StockResultEvent event) {
        try {
            LocalDateTime now = LocalDateTime.now();
            outboxEventRepository.save(OutboxEvent.builder()
                    .id(event.eventId().toString())
                    .eventType("stock-result.v1")
                    .topic(stockResultTopic)
                    .messageKey(event.orderId().toString())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .nextAttemptAt(now)
                    .createdAt(now)
                    .build());
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize stock-result event", exception);
        }
    }
}
