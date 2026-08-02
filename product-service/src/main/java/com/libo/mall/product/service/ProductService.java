package com.libo.mall.product.service;

import com.libo.mall.product.dto.CreateProductRequest;
import com.libo.mall.product.dto.ProductResponse;
import com.libo.mall.product.dto.UpdateProductRequest;

import java.util.List;

public interface ProductService {

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id);

    List<ProductResponse> getProductsByIds(List<Long> ids);

    List<ProductResponse> getAvailableProducts();

    boolean isProductInStock(Long productId, Integer quantity);

    void decreaseStock(Long productId, Integer quantity);

    void increaseStock(Long productId, Integer quantity);
}
