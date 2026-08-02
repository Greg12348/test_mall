package com.libo.mall.product.controller;

import com.libo.mall.product.dto.ApiResponse;
import com.libo.mall.product.dto.CreateProductRequest;
import com.libo.mall.product.dto.ProductResponse;
import com.libo.mall.product.dto.UpdateProductRequest;
import com.libo.mall.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        ApiResponse<List<ProductResponse>> response = ApiResponse.<List<ProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Products retrieved successfully")
                .data(products)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        HttpStatus status = HttpStatus.OK;
        ApiResponse<ProductResponse> response = ApiResponse.<ProductResponse>builder()
                .status(status.value())
                .message("Product retrieved successfully")
                .data(product)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestBody CreateProductRequest request
    ) {
        ProductResponse product = productService.createProduct(request);
        HttpStatus status = HttpStatus.CREATED;
        ApiResponse<ProductResponse> response = ApiResponse.<ProductResponse>builder()
                .status(status.value())
                .message("Product created successfully")
                .data(product)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestBody UpdateProductRequest request
    ) {
        ProductResponse product = productService.updateProduct(id, request);
        HttpStatus status = HttpStatus.OK;
        ApiResponse<ProductResponse> response = ApiResponse.<ProductResponse>builder()
                .status(status.value())
                .message("Product updated successfully")
                .data(product)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        HttpStatus status = HttpStatus.OK;
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(status.value())
                .message("Product deleted successfully")
                .data(null)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByIds(
            @RequestParam List<Long> ids
    ) {
        List<ProductResponse> products = productService.getProductsByIds(ids);
        HttpStatus status = HttpStatus.OK;
        ApiResponse<List<ProductResponse>> response = ApiResponse.<List<ProductResponse>>builder()
                .status(status.value())
                .message("Products retrieved successfully")
                .data(products)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAvailableProducts() {
        List<ProductResponse> products = productService.getAvailableProducts();
        HttpStatus status = HttpStatus.OK;
        ApiResponse<List<ProductResponse>> response = ApiResponse.<List<ProductResponse>>builder()
                .status(status.value())
                .message("Available products retrieved successfully")
                .data(products)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<Boolean>> isProductInStock(
            @PathVariable Long id,
            @RequestParam Integer quantity
    ) {
        boolean inStock = productService.isProductInStock(id, quantity);
        HttpStatus status = HttpStatus.OK;
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .status(status.value())
                .message("Product stock checked successfully")
                .data(inStock)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/{id}/stock/decrease")
    public ResponseEntity<ApiResponse<Void>> decreaseStock(
            @PathVariable Long id,
            @RequestParam Integer quantity
    ) {
        productService.decreaseStock(id, quantity);
        HttpStatus status = HttpStatus.OK;
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(status.value())
                .message("Product stock decreased successfully")
                .data(null)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/{id}/stock/increase")
    public ResponseEntity<ApiResponse<Void>> increaseStock(
            @PathVariable Long id,
            @RequestParam Integer quantity
    ) {
        productService.increaseStock(id, quantity);
        HttpStatus status = HttpStatus.OK;
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(status.value())
                .message("Product stock increased successfully")
                .data(null)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
