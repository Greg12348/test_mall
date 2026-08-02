package com.libo.mall.product.outbox;

import com.libo.mall.product.event.StockResultEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductOutboxServiceTest {

    @Mock
    private OutboxEventRepository repository;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeAndStorePendingStockResult() throws Exception {
        UUID eventId = UUID.randomUUID();
        StockResultEvent event = new StockResultEvent(
                eventId, UUID.randomUUID(), Instant.now(), 10L, true, null
        );
        when(objectMapper.writeValueAsString(event)).thenReturn("{payload}");
        ProductOutboxService service = new ProductOutboxService(
                repository, objectMapper, "stock-result.v1"
        );

        service.saveStockResult(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertEquals(eventId.toString(), saved.getId());
        assertEquals("stock-result.v1", saved.getEventType());
        assertEquals("stock-result.v1", saved.getTopic());
        assertEquals("10", saved.getMessageKey());
        assertEquals("{payload}", saved.getPayload());
        assertEquals(OutboxStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getRetryCount());
    }
}
