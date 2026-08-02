package com.libo.mall.product.messaging;

import com.libo.mall.product.event.OrderCreatedEvent;
import com.libo.mall.product.event.StockResultEvent;
import com.libo.mall.product.outbox.ProductOutboxService;
import com.libo.mall.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final ProductRepository productRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ProductOutboxService productOutboxService;

    public OrderCreatedConsumer(
            ProductRepository productRepository,
            ProcessedEventRepository processedEventRepository,
            ProductOutboxService productOutboxService
    ) {
        this.productRepository = productRepository;
        this.processedEventRepository = processedEventRepository;
        this.productOutboxService = productOutboxService;
    }

    @KafkaListener(topics = "${mall.kafka.topics.order-created}")
    @Transactional
    public void consume(OrderCreatedEvent event) {
        validate(event);
        String sourceEventId = event.eventId().toString();
        if (processedEventRepository.existsById(sourceEventId)) {
            log.info("Skipping duplicate order-created event. eventId={}", sourceEventId);
            return;
        }

        int updatedRows = productRepository.reserveStock(event.productId(), event.quantity());
        boolean reserved = updatedRows == 1;
        String reason = null;

        if (!reserved) {
            reason = productRepository.existsById(event.productId())
                    ? "INSUFFICIENT_STOCK"
                    : "PRODUCT_NOT_FOUND";
        }

        processedEventRepository.save(new ProcessedEvent(
                sourceEventId,
                "order-created.v1",
                LocalDateTime.now()
        ));

        StockResultEvent result = new StockResultEvent(
                UUID.randomUUID(),
                event.eventId(),
                Instant.now(),
                event.orderId(),
                reserved,
                reason
        );
        productOutboxService.saveStockResult(result);

        log.info(
                "Processed order-created event. eventId={}, orderId={}, reserved={}, reason={}",
                sourceEventId, event.orderId(), reserved, reason
        );
    }

    private void validate(OrderCreatedEvent event) {
        if (event.eventId() == null || event.orderId() == null || event.productId() == null) {
            throw new IllegalArgumentException("Order-created event identifiers must not be null");
        }
        if (event.quantity() == null || event.quantity() <= 0) {
            throw new IllegalArgumentException("Order-created event quantity must be positive");
        }
    }
}
