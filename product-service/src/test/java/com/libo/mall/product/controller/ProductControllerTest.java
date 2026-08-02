package com.libo.mall.product.controller;

import com.libo.mall.product.dto.CreateProductRequest;
import com.libo.mall.product.dto.ProductResponse;
import com.libo.mall.product.dto.UpdateProductRequest;
import com.libo.mall.product.exception.InsufficientStockException;
import com.libo.mall.product.exception.ProductNotFoundException;
import com.libo.mall.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void shouldReturnAllProducts() throws Exception {
        ProductResponse keyboard = new ProductResponse(
                1L,
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                12
        );
        ProductResponse mouse = new ProductResponse(
                2L,
                "Mouse",
                "Wireless mouse",
                new BigDecimal("49.99"),
                25
        );

        when(productService.getAllProducts()).thenReturn(List.of(keyboard, mouse));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Products retrieved successfully"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Keyboard"))
                .andExpect(jsonPath("$.data[0].description").value("Mechanical keyboard"))
                .andExpect(jsonPath("$.data[0].price").value(99.99))
                .andExpect(jsonPath("$.data[0].stock").value(12))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("Mouse"));
    }

    @Test
    void shouldReturnProductById() throws Exception {
        ProductResponse product = new ProductResponse(
                1L,
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                12
        );

        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Product retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Keyboard"))
                .andExpect(jsonPath("$.data.description").value("Mechanical keyboard"))
                .andExpect(jsonPath("$.data.price").value(99.99))
                .andExpect(jsonPath("$.data.stock").value(12));
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/products/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldCreateProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "Monitor",
                "4K monitor",
                new BigDecimal("299.99"),
                8
        );
        ProductResponse product = new ProductResponse(
                3L,
                "Monitor",
                "4K monitor",
                new BigDecimal("299.99"),
                8
        );

        when(productService.createProduct(request)).thenReturn(product);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Monitor",
                                  "description": "4K monitor",
                                  "price": 299.99,
                                  "stock": 8
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.name").value("Monitor"));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest(
                "Updated Monitor",
                "Updated description",
                new BigDecimal("249.99"),
                6
        );
        ProductResponse product = new ProductResponse(
                3L,
                "Updated Monitor",
                "Updated description",
                new BigDecimal("249.99"),
                6
        );

        when(productService.updateProduct(3L, request)).thenReturn(product);

        mockMvc.perform(put("/products/{id}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Monitor",
                                  "description": "Updated description",
                                  "price": 249.99,
                                  "stock": 6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Product updated successfully"))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.name").value("Updated Monitor"));
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct(3L);

        mockMvc.perform(delete("/products/{id}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Product deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldReturnProductsByIds() throws Exception {
        ProductResponse keyboard = new ProductResponse(
                1L,
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                12
        );
        ProductResponse mouse = new ProductResponse(
                2L,
                "Mouse",
                "Wireless mouse",
                new BigDecimal("49.99"),
                25
        );

        when(productService.getProductsByIds(List.of(1L, 2L))).thenReturn(List.of(keyboard, mouse));

        mockMvc.perform(get("/products/batch").param("ids", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Products retrieved successfully"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void shouldReturnAvailableProducts() throws Exception {
        ProductResponse keyboard = new ProductResponse(
                1L,
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                12
        );

        when(productService.getAvailableProducts()).thenReturn(List.of(keyboard));

        mockMvc.perform(get("/products/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Available products retrieved successfully"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldCheckProductStock() throws Exception {
        when(productService.isProductInStock(1L, 2)).thenReturn(true);

        mockMvc.perform(get("/products/{id}/stock", 1L).param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Product stock checked successfully"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void shouldDecreaseStock() throws Exception {
        doNothing().when(productService).decreaseStock(1L, 2);

        mockMvc.perform(post("/products/{id}/stock/decrease", 1L).param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Product stock decreased successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldIncreaseStock() throws Exception {
        doNothing().when(productService).increaseStock(1L, 2);

        mockMvc.perform(post("/products/{id}/stock/increase", 1L).param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Product stock increased successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldReturnConflictWhenStockIsInsufficient() throws Exception {
        doThrow(new InsufficientStockException(1L, 20, 12))
                .when(productService)
                .decreaseStock(1L, 20);

        mockMvc.perform(post("/products/{id}/stock/decrease", 1L).param("quantity", "20"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product id: 1. Requested: 20, available: 12"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

}
