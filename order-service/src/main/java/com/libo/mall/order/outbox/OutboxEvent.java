package com.libo.mall.order.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_ready", columnList = "status,next_attempt_at,created_at"),
                @Index(name = "idx_outbox_processing", columnList = "status,processing_started_at")
        }
)
public class OutboxEvent {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(nullable = false, length = 200)
    private String messageKey;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;
}
