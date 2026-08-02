package com.libo.mall.order.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Collection<OutboxStatus> statuses,
            LocalDateTime now,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("""
            update OutboxEvent event
               set event.status = :processing,
                   event.processingStartedAt = :now
             where event.id = :id
               and event.status in :claimableStatuses
               and event.nextAttemptAt <= :now
            """)
    int claim(
            @Param("id") String id,
            @Param("claimableStatuses") Collection<OutboxStatus> claimableStatuses,
            @Param("processing") OutboxStatus processing,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("""
            update OutboxEvent event
               set event.status = :pending,
                   event.processingStartedAt = null,
                   event.nextAttemptAt = :now
             where event.status = :processing
               and event.processingStartedAt < :staleBefore
            """)
    int releaseStaleClaims(
            @Param("processing") OutboxStatus processing,
            @Param("pending") OutboxStatus pending,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("""
            update OutboxEvent event
               set event.status = :published,
                   event.publishedAt = :now,
                   event.processingStartedAt = null,
                   event.lastError = null
             where event.id = :id
               and event.status = :processing
            """)
    int markPublished(
            @Param("id") String id,
            @Param("processing") OutboxStatus processing,
            @Param("published") OutboxStatus published,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("""
            update OutboxEvent event
               set event.status = :failed,
                   event.retryCount = event.retryCount + 1,
                   event.nextAttemptAt = :nextAttemptAt,
                   event.processingStartedAt = null,
                   event.lastError = :lastError
             where event.id = :id
               and event.status = :processing
            """)
    int markFailed(
            @Param("id") String id,
            @Param("processing") OutboxStatus processing,
            @Param("failed") OutboxStatus failed,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("lastError") String lastError
    );
}
