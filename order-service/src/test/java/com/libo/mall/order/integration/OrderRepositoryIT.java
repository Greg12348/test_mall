package com.libo.mall.order.integration;

import com.libo.mall.order.entity.Order;
import com.libo.mall.order.entity.OrderStatus;
import com.libo.mall.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryIT extends MySqlIntegrationTest {

    @Autowired
    private OrderRepository repository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndReadOrderUsingMySql() {
        LocalDateTime now = LocalDateTime.now();
        Order saved = repository.saveAndFlush(Order.builder()
                .userId(1L).productId(2L).quantity(3)
                .totalAmount(new BigDecimal("75.00"))
                .status(OrderStatus.PENDING).createdAt(now).updatedAt(now)
                .build());
        entityManager.clear();

        Order reloaded = repository.findById(saved.getId()).orElseThrow();

        assertNotNull(reloaded.getId());
        assertEquals(1L, reloaded.getUserId());
        assertEquals(new BigDecimal("75.00"), reloaded.getTotalAmount());
        assertEquals(OrderStatus.PENDING, reloaded.getStatus());
    }
}
