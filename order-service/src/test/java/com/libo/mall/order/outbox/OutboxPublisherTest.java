package com.libo.mall.order.outbox;

import com.libo.mall.order.event.OrderCreatedEvent;
import com.libo.mall.order.messaging.OrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository repository;
    @Mock
    private OrderEventPublisher eventPublisher;
    @Mock
    private ObjectMapper objectMapper;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(repository, eventPublisher, objectMapper, 100, 1, 60);
    }

    @Test
    void shouldClaimPublishAndMarkEventPublished() throws Exception {
        OutboxEvent event = event();
        OrderCreatedEvent payload = new OrderCreatedEvent(
                UUID.randomUUID(), Instant.now(), 10L, 20L, 2
        );
        when(repository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(repository.claim(eq(event.getId()), anyList(), eq(OutboxStatus.PROCESSING),
                any(LocalDateTime.class))).thenReturn(1);
        when(objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class)).thenReturn(payload);
        when(eventPublisher.publishOrderCreated(event.getTopic(), event.getMessageKey(), payload))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPendingEvents();

        verify(repository).markPublished(eq(event.getId()), eq(OutboxStatus.PROCESSING),
                eq(OutboxStatus.PUBLISHED), any(LocalDateTime.class));
    }

    @Test
    void shouldSkipEventWhenAnotherPublisherOwnsClaim() {
        OutboxEvent event = event();
        when(repository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(repository.claim(eq(event.getId()), anyList(), eq(OutboxStatus.PROCESSING),
                any(LocalDateTime.class))).thenReturn(0);

        publisher.publishPendingEvents();

        verify(eventPublisher, never()).publishOrderCreated(any(), any(), any());
        verify(repository, never()).markPublished(any(), any(), any(), any());
    }

    @Test
    void shouldMarkEventFailedWhenKafkaPublishFails() throws Exception {
        OutboxEvent event = event();
        OrderCreatedEvent payload = new OrderCreatedEvent(
                UUID.randomUUID(), Instant.now(), 10L, 20L, 2
        );
        when(repository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(repository.claim(eq(event.getId()), anyList(), eq(OutboxStatus.PROCESSING),
                any(LocalDateTime.class))).thenReturn(1);
        when(objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class)).thenReturn(payload);
        when(eventPublisher.publishOrderCreated(event.getTopic(), event.getMessageKey(), payload))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")));

        publisher.publishPendingEvents();

        verify(repository).markFailed(eq(event.getId()), eq(OutboxStatus.PROCESSING),
                eq(OutboxStatus.FAILED), any(LocalDateTime.class), eq("Kafka unavailable"));
    }

    private OutboxEvent event() {
        LocalDateTime now = LocalDateTime.now();
        return OutboxEvent.builder()
                .id(UUID.randomUUID().toString())
                .eventType("order-created.v1")
                .topic("order-created.v1")
                .messageKey("10")
                .payload("{payload}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .build();
    }
}
