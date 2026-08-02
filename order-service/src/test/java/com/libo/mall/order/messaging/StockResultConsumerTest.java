package com.libo.mall.order.messaging;

import com.libo.mall.order.entity.Order;
import com.libo.mall.order.entity.OrderStatus;
import com.libo.mall.order.event.StockResultEvent;
import com.libo.mall.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockResultConsumerTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    private StockResultConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new StockResultConsumer(orderRepository, processedEventRepository);
    }

    @Test
    void shouldMarkPendingOrderAsStockReserved() {
        Order order = order(OrderStatus.PENDING);
        StockResultEvent event = event(true, null);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        consumer.consume(event);

        assertEquals(OrderStatus.STOCK_RESERVED, order.getStatus());
        verify(orderRepository).save(order);
        verify(processedEventRepository).save(org.mockito.ArgumentMatchers.any(ProcessedEvent.class));
    }

    @Test
    void shouldMarkPendingOrderAsRejected() {
        Order order = order(OrderStatus.PENDING);
        StockResultEvent event = event(false, "INSUFFICIENT_STOCK");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        consumer.consume(event);

        assertEquals(OrderStatus.REJECTED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldSkipDuplicateEvent() {
        StockResultEvent event = event(true, null);
        when(processedEventRepository.existsById(event.eventId().toString())).thenReturn(true);

        consumer.consume(event);

        verify(orderRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        verify(processedEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldAcceptRepeatedMatchingOrderStatusWithoutSavingOrderAgain() {
        Order order = order(OrderStatus.STOCK_RESERVED);
        StockResultEvent event = event(true, null);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        consumer.consume(event);

        verify(orderRepository, never()).save(order);
        verify(processedEventRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectConflictingTransition() {
        Order order = order(OrderStatus.REJECTED);
        StockResultEvent event = event(true, null);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> consumer.consume(event));
        verify(processedEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldFailWhenOrderDoesNotExist() {
        StockResultEvent event = event(true, null);
        when(orderRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> consumer.consume(event));
    }

    @Test
    void shouldRejectInvalidSuccessAndFailureEvents() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.consume(event(true, "unexpected")));
        assertThrows(IllegalArgumentException.class,
                () -> consumer.consume(event(false, " ")));
    }

    private StockResultEvent event(boolean reserved, String reason) {
        return new StockResultEvent(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), 10L, reserved, reason
        );
    }

    private Order order(OrderStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 12, 0);
        return Order.builder()
                .id(10L)
                .userId(1L)
                .productId(2L)
                .quantity(2)
                .totalAmount(new BigDecimal("50.00"))
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
