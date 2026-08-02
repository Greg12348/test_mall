package com.libo.mall.product.messaging;

import com.libo.mall.product.event.OrderCreatedEvent;
import com.libo.mall.product.event.StockResultEvent;
import com.libo.mall.product.outbox.ProductOutboxService;
import com.libo.mall.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private ProductOutboxService productOutboxService;

    private OrderCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderCreatedConsumer(
                productRepository, processedEventRepository, productOutboxService
        );
    }

    @Test
    void shouldReserveStockAndWriteSuccessfulResult() {
        OrderCreatedEvent event = event(2);
        when(productRepository.reserveStock(20L, 2)).thenReturn(1);

        consumer.consume(event);

        verify(processedEventRepository).save(org.mockito.ArgumentMatchers.any(ProcessedEvent.class));
        ArgumentCaptor<StockResultEvent> captor = ArgumentCaptor.forClass(StockResultEvent.class);
        verify(productOutboxService).saveStockResult(captor.capture());
        assertTrue(captor.getValue().reserved());
        assertEquals(event.eventId(), captor.getValue().causationId());
        assertEquals(10L, captor.getValue().orderId());
        assertEquals(null, captor.getValue().reason());
    }

    @Test
    void shouldReportInsufficientStock() {
        OrderCreatedEvent event = event(20);
        when(productRepository.reserveStock(20L, 20)).thenReturn(0);
        when(productRepository.existsById(20L)).thenReturn(true);

        consumer.consume(event);

        ArgumentCaptor<StockResultEvent> captor = ArgumentCaptor.forClass(StockResultEvent.class);
        verify(productOutboxService).saveStockResult(captor.capture());
        assertFalse(captor.getValue().reserved());
        assertEquals("INSUFFICIENT_STOCK", captor.getValue().reason());
    }

    @Test
    void shouldReportMissingProduct() {
        OrderCreatedEvent event = event(2);
        when(productRepository.reserveStock(20L, 2)).thenReturn(0);
        when(productRepository.existsById(20L)).thenReturn(false);

        consumer.consume(event);

        ArgumentCaptor<StockResultEvent> captor = ArgumentCaptor.forClass(StockResultEvent.class);
        verify(productOutboxService).saveStockResult(captor.capture());
        assertEquals("PRODUCT_NOT_FOUND", captor.getValue().reason());
    }

    @Test
    void shouldSkipDuplicateEvent() {
        OrderCreatedEvent event = event(2);
        when(processedEventRepository.existsById(event.eventId().toString())).thenReturn(true);

        consumer.consume(event);

        verify(productRepository, never()).reserveStock(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
        verify(productOutboxService, never()).saveStockResult(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectInvalidEvent() {
        OrderCreatedEvent invalid = new OrderCreatedEvent(null, Instant.now(), 10L, 20L, 0);

        assertThrows(IllegalArgumentException.class, () -> consumer.consume(invalid));
        verify(processedEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private OrderCreatedEvent event(int quantity) {
        return new OrderCreatedEvent(UUID.randomUUID(), Instant.now(), 10L, 20L, quantity);
    }
}
