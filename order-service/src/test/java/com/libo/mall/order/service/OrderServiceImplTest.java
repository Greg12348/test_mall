package com.libo.mall.order.service;

import com.libo.mall.order.client.ProductClient;
import com.libo.mall.order.client.dto.ProductResponse;
import com.libo.mall.order.dto.ApiResponse;
import com.libo.mall.order.dto.CreateOrderRequest;
import com.libo.mall.order.dto.OrderResponse;
import com.libo.mall.order.entity.Order;
import com.libo.mall.order.entity.OrderStatus;
import com.libo.mall.order.exception.OrderNotFoundException;
import com.libo.mall.order.outbox.OrderOutboxService;
import com.libo.mall.order.repository.OrderRepository;
import com.libo.mall.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductClient productClient;
    @Mock
    private OrderOutboxService orderOutboxService;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, productClient, orderOutboxService);
    }

    @Test
    void shouldCreateOrderUsingCurrentProductPriceAndWriteOutboxEvent() {
        ProductResponse product = new ProductResponse(
                2L, "Keyboard", "Mechanical", new BigDecimal("25.00"), 10
        );
        when(productClient.getProductById(2L)).thenReturn(
                ApiResponse.<ProductResponse>builder().status(200).data(product).build()
        );
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(Order.class)))
                .thenAnswer(invocation -> {
                    Order saved = invocation.getArgument(0);
                    saved.setId(10L);
                    return saved;
                });

        OrderResponse result = orderService.createOrder(
                new CreateOrderRequest(1L, 2L, 3, new BigDecimal("1.00"))
        );

        assertEquals(10L, result.id());
        assertEquals(new BigDecimal("75.00"), result.totalAmount());
        assertEquals(OrderStatus.PENDING, result.status());
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderOutboxService).saveOrderCreated(captor.capture());
        assertEquals(10L, captor.getValue().getId());
    }

    @Test
    void shouldNotSaveOrderWhenProductLookupFails() {
        when(productClient.getProductById(2L)).thenThrow(new IllegalStateException("unavailable"));

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(
                new CreateOrderRequest(1L, 2L, 3, null)
        ));

        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(orderOutboxService, never()).saveOrderCreated(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturnOrderById() {
        Order order = order(10L, OrderStatus.STOCK_RESERVED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResponse result = orderService.getOrderById(10L);

        assertEquals(10L, result.id());
        assertEquals(OrderStatus.STOCK_RESERVED, result.status());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());
    }

    @Test
    void shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(99L));
    }

    private Order order(Long id, OrderStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 12, 0);
        return Order.builder()
                .id(id)
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
