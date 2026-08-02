package com.libo.mall.order.outbox;

import com.libo.mall.order.entity.Order;
import com.libo.mall.order.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderOutboxServiceTest {

    @Mock
    private OutboxEventRepository repository;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeAndStorePendingOrderCreatedEvent() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{payload}");
        OrderOutboxService service = new OrderOutboxService(
                repository, objectMapper, "order-created.v1"
        );
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 12, 0);
        Order order = Order.builder()
                .id(10L).userId(1L).productId(2L).quantity(3)
                .totalAmount(new BigDecimal("75.00"))
                .status(OrderStatus.PENDING).createdAt(now).updatedAt(now)
                .build();

        service.saveOrderCreated(order);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertEquals("order-created.v1", saved.getEventType());
        assertEquals("order-created.v1", saved.getTopic());
        assertEquals("10", saved.getMessageKey());
        assertEquals("{payload}", saved.getPayload());
        assertEquals(OutboxStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getRetryCount());
    }
}
