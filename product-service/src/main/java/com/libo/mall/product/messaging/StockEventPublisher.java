package com.libo.mall.product.messaging;

import com.libo.mall.product.event.StockResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class StockEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(StockEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public StockEventPublisher(
            @Qualifier("kafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Object>> publish(
            String topic,
            String key,
            StockResultEvent event
    ) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Failed to publish stock-result event. orderId={}", event.orderId(), exception);
                return;
            }
            log.info(
                    "Published stock-result event. orderId={}, topic={}, partition={}, offset={}",
                    event.orderId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
        return future;
    }
}
