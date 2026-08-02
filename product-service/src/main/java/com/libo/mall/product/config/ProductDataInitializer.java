package com.libo.mall.product.config;

import com.libo.mall.product.entity.Product;
import com.libo.mall.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class ProductDataInitializer {

    @Bean
    CommandLineRunner initializeProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                productRepository.saveAll(List.of(
                        Product.builder()
                                .name("Laptop")
                                .description("A practice laptop product")
                                .price(new BigDecimal("999.99"))
                                .stock(10)
                                .build(),
                        Product.builder()
                                .name("Phone")
                                .description("A practice phone product")
                                .price(new BigDecimal("599.99"))
                                .stock(20)
                                .build(),
                        Product.builder()
                                .name("Headphones")
                                .description("A practice headphones product")
                                .price(new BigDecimal("79.99"))
                                .stock(30)
                                .build()
                ));
            }
        };
    }
}
