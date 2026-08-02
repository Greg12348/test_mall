package com.libo.mall.order.outbox;

import com.libo.mall.order.event.OrderCreatedEvent;
import com.libo.mall.order.messaging.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final List<OutboxStatus> CLAIMABLE_STATUSES =
            List.of(OutboxStatus.PENDING, OutboxStatus.FAILED);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final long sendTimeoutSeconds;
    private final long processingTimeoutSeconds;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OrderEventPublisher orderEventPublisher,
            ObjectMapper objectMapper,
            @Value("${mall.outbox.publisher.batch-size}") int batchSize,
            @Value("${mall.outbox.publisher.send-timeout-seconds}") long sendTimeoutSeconds,
            @Value("${mall.outbox.publisher.processing-timeout-seconds}") long processingTimeoutSeconds
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.sendTimeoutSeconds = sendTimeoutSeconds;
        this.processingTimeoutSeconds = processingTimeoutSeconds;
    }

    @Scheduled(fixedDelayString = "${mall.outbox.publisher.fixed-delay-ms}")
    public void publishPendingEvents() {
        LocalDateTime now = LocalDateTime.now();
        outboxEventRepository.releaseStaleClaims(
                OutboxStatus.PROCESSING,
                OutboxStatus.PENDING,
                now.minusSeconds(processingTimeoutSeconds),
                now
        );

        List<OutboxEvent> events = outboxEventRepository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        CLAIMABLE_STATUSES,
                        now,
                        PageRequest.of(0, batchSize)
                );

        for (OutboxEvent event : events) {
            publishIfClaimed(event);
        }
    }

    private void publishIfClaimed(OutboxEvent event) {
        LocalDateTime claimedAt = LocalDateTime.now();
        int claimed = outboxEventRepository.claim(
                event.getId(),
                CLAIMABLE_STATUSES,
                OutboxStatus.PROCESSING,
                claimedAt
        );
        if (claimed == 0) {
            return;
        }

        try {
            OrderCreatedEvent payload = objectMapper.readValue(
                    event.getPayload(),
                    OrderCreatedEvent.class
            );
            orderEventPublisher.publishOrderCreated(
                            event.getTopic(),
                            event.getMessageKey(),
                            payload
                    )
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);

            outboxEventRepository.markPublished(
                    event.getId(),
                    OutboxStatus.PROCESSING,
                    OutboxStatus.PUBLISHED,
                    LocalDateTime.now()
            );
        } catch (Exception exception) {
            markFailed(event, exception);
        }
    }

    private void markFailed(OutboxEvent event, Exception exception) {
        int nextRetryCount = event.getRetryCount() + 1;
        long delaySeconds = Math.min(300L, 1L << Math.min(nextRetryCount, 8));
        String message = rootMessage(exception);

        outboxEventRepository.markFailed(
                event.getId(),
                OutboxStatus.PROCESSING,
                OutboxStatus.FAILED,
                LocalDateTime.now().plusSeconds(delaySeconds),
                message.substring(0, Math.min(message.length(), 1000))
        );
        log.error(
                "Failed to publish outbox event. eventId={}, retryCount={}, retryInSeconds={}",
                event.getId(), nextRetryCount, delaySeconds, exception
        );
    }

    private String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getName() : current.getMessage();
    }
}
