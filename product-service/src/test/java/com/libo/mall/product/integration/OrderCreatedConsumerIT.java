package com.libo.mall.product.integration;

import com.libo.mall.product.entity.Product;
import com.libo.mall.product.event.OrderCreatedEvent;
import com.libo.mall.product.messaging.OrderCreatedConsumer;
import com.libo.mall.product.messaging.ProcessedEventRepository;
import com.libo.mall.product.outbox.ProductOutboxService;
import com.libo.mall.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderCreatedConsumer.class)
class OrderCreatedConsumerIT extends MySqlIntegrationTest {

    @Autowired
    private OrderCreatedConsumer consumer;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    @Autowired
    private EntityManager entityManager;
    @MockitoBean
    private ProductOutboxService productOutboxService;

    @Test
    void shouldReserveStockPersistProcessedEventAndIgnoreDuplicate() {
        Product product = productRepository.saveAndFlush(Product.builder()
                .name("Integration Product").description("Consumer test")
                .price(new BigDecimal("10.00")).stock(5).build());
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId, Instant.now(), 10L, product.getId(), 2
        );

        consumer.consume(event);
        consumer.consume(event);
        entityManager.flush();
        entityManager.clear();

        assertEquals(3, productRepository.findById(product.getId()).orElseThrow().getStock());
        assertTrue(processedEventRepository.existsById(eventId.toString()));
        verify(productOutboxService, times(1)).saveStockResult(
                org.mockito.ArgumentMatchers.argThat(result -> result.reserved()
                        && result.orderId().equals(10L))
        );
    }
}
