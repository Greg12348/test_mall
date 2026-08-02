package com.libo.mall.product.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;
}
