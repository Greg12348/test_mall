package com.libo.mall.order.messaging;

import com.libo.mall.order.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(
            @Qualifier("kafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Object>> publishOrderCreated(
            String topic,
            String key,
            OrderCreatedEvent event
    ) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
        future
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish order-created event. orderId={}", event.orderId(), exception);
                        return;
                    }

                    log.info(
                            "Published order-created event. orderId={}, topic={}, partition={}, offset={}",
                            event.orderId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
        return future;
    }
}
