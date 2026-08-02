package com.libo.mall.product.service;

import com.libo.mall.product.dto.CreateProductRequest;
import com.libo.mall.product.dto.ProductResponse;
import com.libo.mall.product.dto.UpdateProductRequest;
import com.libo.mall.product.entity.Product;
import com.libo.mall.product.exception.InsufficientStockException;
import com.libo.mall.product.exception.ProductNotFoundException;
import com.libo.mall.product.repository.ProductRepository;
import com.libo.mall.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository);
    }

    @Test
    void shouldReturnAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product(1L, "Laptop", 10)));

        List<ProductResponse> result = productService.getAllProducts();

        assertEquals(1, result.size());
        assertEquals("Laptop", result.getFirst().name());
        assertEquals(10, result.getFirst().stock());
    }

    @Test
    void shouldReturnProductById() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, "Laptop", 10)));

        ProductResponse result = productService.getProductById(1L);

        assertEquals(1L, result.id());
        assertEquals(new BigDecimal("25.00"), result.price());
    }

    @Test
    void shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void shouldCreateProduct() {
        CreateProductRequest request = new CreateProductRequest(
                "Keyboard", "Mechanical", new BigDecimal("99.00"), 7
        );
        when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                .thenAnswer(invocation -> {
                    Product saved = invocation.getArgument(0);
                    saved.setId(3L);
                    return saved;
                });

        ProductResponse result = productService.createProduct(request);

        assertEquals(3L, result.id());
        assertEquals("Keyboard", result.name());
        assertEquals(7, result.stock());
    }

    @Test
    void shouldUpdateProduct() {
        Product existing = product(1L, "Old", 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        ProductResponse result = productService.updateProduct(1L,
                new UpdateProductRequest("New", "Updated", new BigDecimal("30.00"), 9));

        assertEquals("New", result.name());
        assertEquals("Updated", result.description());
        assertEquals(new BigDecimal("30.00"), result.price());
        assertEquals(9, result.stock());
    }

    @Test
    void shouldDeleteProduct() {
        Product existing = product(1L, "Laptop", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(1L);

        verify(productRepository).delete(existing);
    }

    @Test
    void shouldReturnProductsByIds() {
        when(productRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                product(1L, "Laptop", 10), product(2L, "Phone", 0)
        ));

        List<ProductResponse> result = productService.getProductsByIds(List.of(1L, 2L));

        assertEquals(List.of(1L, 2L), result.stream().map(ProductResponse::id).toList());
    }

    @Test
    void shouldReturnOnlyAvailableProducts() {
        when(productRepository.findAll()).thenReturn(List.of(
                product(1L, "Laptop", 10), product(2L, "Phone", 0)
        ));

        List<ProductResponse> result = productService.getAvailableProducts();

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().id());
    }

    @Test
    void shouldReportWhetherRequestedStockIsAvailable() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, "Laptop", 5)));

        assertTrue(productService.isProductInStock(1L, 5));
        assertFalse(productService.isProductInStock(1L, 6));
    }

    @Test
    void shouldDecreaseStock() {
        Product existing = product(1L, "Laptop", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.decreaseStock(1L, 3);

        assertEquals(7, existing.getStock());
        verify(productRepository).save(existing);
    }

    @Test
    void shouldRejectDecreaseWhenStockIsInsufficient() {
        Product existing = product(1L, "Laptop", 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(InsufficientStockException.class, () -> productService.decreaseStock(1L, 3));
        assertEquals(2, existing.getStock());
        verify(productRepository, never()).save(existing);
    }

    @Test
    void shouldIncreaseStock() {
        Product existing = product(1L, "Laptop", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.increaseStock(1L, 4);

        assertEquals(14, existing.getStock());
        verify(productRepository).save(existing);
    }

    @Test
    void shouldPassCreatedValuesToRepository() {
        CreateProductRequest request = new CreateProductRequest(
                "Mouse", "Wireless", new BigDecimal("45.50"), 8
        );
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        productService.createProduct(request);

        Product saved = captor.getValue();
        assertEquals("Mouse", saved.getName());
        assertEquals("Wireless", saved.getDescription());
        assertEquals(new BigDecimal("45.50"), saved.getPrice());
        assertEquals(8, saved.getStock());
    }

    private Product product(Long id, String name, int stock) {
        return Product.builder()
                .id(id)
                .name(name)
                .description("Description")
                .price(new BigDecimal("25.00"))
                .stock(stock)
                .build();
    }
}
