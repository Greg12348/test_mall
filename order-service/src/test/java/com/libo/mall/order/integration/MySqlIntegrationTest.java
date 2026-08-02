package com.libo.mall.order.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

abstract class MySqlIntegrationTest {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("mall_order_it")
            .withUsername("mall")
            .withPassword("mall");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
