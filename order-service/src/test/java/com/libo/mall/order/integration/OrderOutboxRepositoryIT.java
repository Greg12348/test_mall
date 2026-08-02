package com.libo.mall.order.integration;

import com.libo.mall.order.outbox.OutboxEvent;
import com.libo.mall.order.outbox.OutboxEventRepository;
import com.libo.mall.order.outbox.OutboxStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderOutboxRepositoryIT extends MySqlIntegrationTest {

    @Autowired
    private OutboxEventRepository repository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindClaimAndPublishReadyEvent() {
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent event = repository.saveAndFlush(event(now.minusSeconds(1)));

        List<OutboxEvent> ready = repository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(OutboxStatus.PENDING, OutboxStatus.FAILED), now,
                        PageRequest.of(0, 10));
        assertEquals(List.of(event.getId()), ready.stream().map(OutboxEvent::getId).toList());

        assertEquals(1, repository.claim(event.getId(),
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                OutboxStatus.PROCESSING, now));
        assertEquals(1, repository.markPublished(event.getId(), OutboxStatus.PROCESSING,
                OutboxStatus.PUBLISHED, now.plusSeconds(1)));
        entityManager.clear();

        OutboxEvent published = repository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PUBLISHED, published.getStatus());
        assertNotNull(published.getPublishedAt());
    }

    @Test
    void shouldReleaseStaleClaimAndRecordFailure() {
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent event = event(now.minusMinutes(2));
        event.setStatus(OutboxStatus.PROCESSING);
        event.setProcessingStartedAt(now.minusMinutes(2));
        repository.saveAndFlush(event);

        assertEquals(1, repository.releaseStaleClaims(OutboxStatus.PROCESSING,
                OutboxStatus.PENDING, now.minusSeconds(60), now));
        assertEquals(1, repository.claim(event.getId(), List.of(OutboxStatus.PENDING),
                OutboxStatus.PROCESSING, now));
        assertEquals(1, repository.markFailed(event.getId(), OutboxStatus.PROCESSING,
                OutboxStatus.FAILED, now.plusSeconds(2), "Kafka unavailable"));
        entityManager.clear();

        OutboxEvent failed = repository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.FAILED, failed.getStatus());
        assertEquals(1, failed.getRetryCount());
        assertEquals("Kafka unavailable", failed.getLastError());
    }

    private OutboxEvent event(LocalDateTime time) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID().toString()).eventType("order-created.v1")
                .topic("order-created.v1").messageKey("10").payload("{}")
                .status(OutboxStatus.PENDING).retryCount(0)
                .nextAttemptAt(time).createdAt(time).build();
    }
}
