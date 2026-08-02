package com.libo.mall.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    KafkaAdmin.NewTopics mallTopics(
            @Value("${mall.kafka.topics.order-created}") String orderCreatedTopic,
            @Value("${mall.kafka.topics.stock-result}") String stockResultTopic,
            @Value("${mall.kafka.topics.partitions}") int partitions,
            @Value("${mall.kafka.topics.replicas}") int replicas
    ) {
        return new KafkaAdmin.NewTopics(
                topic(orderCreatedTopic, partitions, replicas),
                topic(orderCreatedTopic + ".DLT", partitions, replicas),
                topic(stockResultTopic, partitions, replicas),
                topic(stockResultTopic + ".DLT", partitions, replicas)
        );
    }

    @Bean
    DltKafkaResources dltKafkaResources(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> properties = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true
        );
        return new DltKafkaResources(properties);
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            @Qualifier("kafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            DltKafkaResources dltKafkaResources,
            @Value("${mall.kafka.consumer.retry.interval-ms}") long retryIntervalMs,
            @Value("${mall.kafka.consumer.retry.max-attempts}") long maxAttempts
    ) {
        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, dltKafkaResources.kafkaTemplate());
        templates.put(Object.class, kafkaTemplate);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                templates,
                (record, exception) -> new TopicPartition(
                        record.topic() + ".DLT",
                        record.partition()
                )
        );
        recoverer.setFailIfSendResultIsError(true);

        long retryCount = Math.max(0, maxAttempts - 1);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryIntervalMs, retryCount)
        );
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }

    private NewTopic topic(String name, int partitions, int replicas) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    static final class DltKafkaResources implements DisposableBean {

        private final DefaultKafkaProducerFactory<String, byte[]> producerFactory;
        private final KafkaTemplate<String, byte[]> kafkaTemplate;

        DltKafkaResources(Map<String, Object> properties) {
            this.producerFactory = new DefaultKafkaProducerFactory<>(properties);
            this.kafkaTemplate = new KafkaTemplate<>(producerFactory);
        }

        KafkaTemplate<String, byte[]> kafkaTemplate() {
            return kafkaTemplate;
        }

        @Override
        public void destroy() {
            producerFactory.destroy();
        }
    }
}
