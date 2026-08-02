package com.libo.mall.order.messaging;

import com.libo.mall.order.entity.Order;
import com.libo.mall.order.entity.OrderStatus;
import com.libo.mall.order.event.StockResultEvent;
import com.libo.mall.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class StockResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockResultConsumer.class);

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;

    public StockResultConsumer(
            OrderRepository orderRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = "${mall.kafka.topics.stock-result}")
    @Transactional
    public void consume(StockResultEvent event) {
        validate(event);
        String eventId = event.eventId().toString();

        if (processedEventRepository.existsById(eventId)) {
            log.info("Skipping duplicate stock-result event. eventId={}", eventId);
            return;
        }

        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found for stock-result event: " + event.orderId()
                ));

        OrderStatus targetStatus = event.reserved()
                ? OrderStatus.STOCK_RESERVED
                : OrderStatus.REJECTED;
        applyTransition(order, targetStatus, event);

        processedEventRepository.save(new ProcessedEvent(
                eventId,
                "stock-result.v1",
                LocalDateTime.now()
        ));

        log.info(
                "Processed stock-result event. eventId={}, orderId={}, status={}, reason={}",
                eventId, event.orderId(), order.getStatus(), event.reason()
        );
    }

    private void applyTransition(
            Order order,
            OrderStatus targetStatus,
            StockResultEvent event
    ) {
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(targetStatus);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            return;
        }

        if (order.getStatus() == targetStatus) {
            return;
        }

        throw new IllegalStateException(
                "Conflicting stock result for order " + event.orderId()
                        + ": current status=" + order.getStatus()
                        + ", requested status=" + targetStatus
        );
    }

    private void validate(StockResultEvent event) {
        if (event.eventId() == null
                || event.causationId() == null
                || event.occurredAt() == null
                || event.orderId() == null) {
            throw new IllegalArgumentException("Stock-result event fields must not be null");
        }
        if (event.reserved() && event.reason() != null) {
            throw new IllegalArgumentException("Successful stock result must not include a failure reason");
        }
        if (!event.reserved() && (event.reason() == null || event.reason().isBlank())) {
            throw new IllegalArgumentException("Rejected stock result must include a failure reason");
        }
    }
}
