package com.libo.mall.order.integration;

import com.libo.mall.order.entity.Order;
import com.libo.mall.order.entity.OrderStatus;
import com.libo.mall.order.event.StockResultEvent;
import com.libo.mall.order.messaging.ProcessedEventRepository;
import com.libo.mall.order.messaging.StockResultConsumer;
import com.libo.mall.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(StockResultConsumer.class)
class StockResultConsumerIT extends MySqlIntegrationTest {

    @Autowired
    private StockResultConsumer consumer;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistStatusTransitionAndIgnoreDuplicate() {
        LocalDateTime now = LocalDateTime.now();
        Order order = orderRepository.saveAndFlush(Order.builder()
                .userId(1L).productId(2L).quantity(2)
                .totalAmount(new BigDecimal("50.00"))
                .status(OrderStatus.PENDING).createdAt(now).updatedAt(now).build());
        UUID eventId = UUID.randomUUID();
        StockResultEvent event = new StockResultEvent(
                eventId, UUID.randomUUID(), Instant.now(), order.getId(), true, null
        );

        consumer.consume(event);
        consumer.consume(event);
        entityManager.flush();
        entityManager.clear();

        assertEquals(OrderStatus.STOCK_RESERVED,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
        assertTrue(processedEventRepository.existsById(eventId.toString()));
    }
}
