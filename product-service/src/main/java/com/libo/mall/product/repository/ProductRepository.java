package com.libo.mall.product.repository;

import com.libo.mall.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Query("""
            update Product product
               set product.stock = product.stock - :quantity
             where product.id = :productId
               and product.stock >= :quantity
            """)
    int reserveStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );
}
