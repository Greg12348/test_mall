package com.libo.mall.order.client;

import com.libo.mall.order.client.dto.ProductResponse;
import com.libo.mall.order.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", url = "${services.product.url}")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ApiResponse<ProductResponse> getProductById(@PathVariable Long id);

    @GetMapping("/products/{id}/stock")
    ApiResponse<Boolean> isProductInStock(
            @PathVariable Long id,
            @RequestParam Integer quantity
    );

    @PostMapping("/products/{id}/stock/decrease")
    ApiResponse<Void> decreaseStock(
            @PathVariable Long id,
            @RequestParam Integer quantity
    );
}
