package com.libo.mall.product.integration;

import com.libo.mall.product.entity.Product;
import com.libo.mall.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryIT extends MySqlIntegrationTest {

    @Autowired
    private ProductRepository repository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndReadProductUsingMySql() {
        Product saved = repository.saveAndFlush(product(10));
        entityManager.clear();

        Product reloaded = repository.findById(saved.getId()).orElseThrow();

        assertEquals("Integration Laptop", reloaded.getName());
        assertEquals(new BigDecimal("999.99"), reloaded.getPrice());
        assertEquals(10, reloaded.getStock());
    }

    @Test
    void shouldReserveStockAtomicallyAndRejectInsufficientReservation() {
        Product saved = repository.saveAndFlush(product(5));

        assertEquals(1, repository.reserveStock(saved.getId(), 3));
        assertEquals(0, repository.reserveStock(saved.getId(), 3));
        entityManager.clear();

        assertEquals(2, repository.findById(saved.getId()).orElseThrow().getStock());
    }

    private Product product(int stock) {
        return Product.builder()
                .name("Integration Laptop")
                .description("MySQL integration test")
                .price(new BigDecimal("999.99"))
                .stock(stock)
                .build();
    }
}
