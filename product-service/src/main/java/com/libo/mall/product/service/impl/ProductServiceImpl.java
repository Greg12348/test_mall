package com.libo.mall.product.service.impl;

import com.libo.mall.product.dto.CreateProductRequest;
import com.libo.mall.product.dto.ProductResponse;
import com.libo.mall.product.dto.UpdateProductRequest;
import com.libo.mall.product.entity.Product;
import com.libo.mall.product.exception.InsufficientStockException;
import com.libo.mall.product.exception.ProductNotFoundException;
import com.libo.mall.product.repository.ProductRepository;
import com.libo.mall.product.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        log.info("Getting all products");
        try {
            List<ProductResponse> products = productRepository.findAll().stream()
                    .map(this::toResponse)
                    .toList();
            log.info("Got all products successfully. count={}", products.size());
            return products;
        } catch (RuntimeException exception) {
            log.error("Failed to get all products", exception);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.info("Getting product by id. productId={}", id);
        try {
            ProductResponse product = toResponse(findProductById(id));
            log.info("Got product successfully. productId={}", id);
            return product;
        } catch (RuntimeException exception) {
            log.error("Failed to get product. productId={}", id, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product. name={}", request.name());
        try {
            Product product = Product.builder()
                    .name(request.name())
                    .description(request.description())
                    .price(request.price())
                    .stock(request.stock())
                    .build();

            ProductResponse response = toResponse(productRepository.save(product));
            log.info("Created product successfully. productId={}", response.id());
            return response;
        } catch (RuntimeException exception) {
            log.error("Failed to create product. name={}", request.name(), exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product. productId={}", id);
        try {
            Product product = findProductById(id);
            product.setName(request.name());
            product.setDescription(request.description());
            product.setPrice(request.price());
            product.setStock(request.stock());

            ProductResponse response = toResponse(productRepository.save(product));
            log.info("Updated product successfully. productId={}", id);
            return response;
        } catch (RuntimeException exception) {
            log.error("Failed to update product. productId={}", id, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product. productId={}", id);
        try {
            Product product = findProductById(id);
            productRepository.delete(product);
            log.info("Deleted product successfully. productId={}", id);
        } catch (RuntimeException exception) {
            log.error("Failed to delete product. productId={}", id, exception);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByIds(List<Long> ids) {
        log.info("Getting products by ids. ids={}", ids);
        try {
            List<ProductResponse> products = productRepository.findAllById(ids).stream()
                    .map(this::toResponse)
                    .toList();
            log.info("Got products by ids successfully. requestedCount={}, foundCount={}", ids.size(), products.size());
            return products;
        } catch (RuntimeException exception) {
            log.error("Failed to get products by ids. ids={}", ids, exception);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAvailableProducts() {
        log.info("Getting available products");
        try {
            List<ProductResponse> products = productRepository.findAll().stream()
                    .filter(product -> product.getStock() > 0)
                    .map(this::toResponse)
                    .toList();
            log.info("Got available products successfully. count={}", products.size());
            return products;
        } catch (RuntimeException exception) {
            log.error("Failed to get available products", exception);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductInStock(Long productId, Integer quantity) {
        log.info("Checking product stock. productId={}, quantity={}", productId, quantity);
        try {
            Product product = findProductById(productId);
            boolean inStock = product.getStock() >= quantity;
            log.info("Checked product stock successfully. productId={}, quantity={}, inStock={}",
                    productId, quantity, inStock);
            return inStock;
        } catch (RuntimeException exception) {
            log.error("Failed to check product stock. productId={}, quantity={}", productId, quantity, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        log.info("Decreasing product stock. productId={}, quantity={}", productId, quantity);
        try {
            Product product = findProductById(productId);
            if (product.getStock() < quantity) {
                throw new InsufficientStockException(productId, quantity, product.getStock());
            }

            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
            log.info("Decreased product stock successfully. productId={}, quantity={}, remainingStock={}",
                    productId, quantity, product.getStock());
        } catch (RuntimeException exception) {
            log.error("Failed to decrease product stock. productId={}, quantity={}", productId, quantity, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void increaseStock(Long productId, Integer quantity) {
        log.info("Increasing product stock. productId={}, quantity={}", productId, quantity);
        try {
            Product product = findProductById(productId);
            product.setStock(product.getStock() + quantity);
            productRepository.save(product);
            log.info("Increased product stock successfully. productId={}, quantity={}, currentStock={}",
                    productId, quantity, product.getStock());
        } catch (RuntimeException exception) {
            log.error("Failed to increase product stock. productId={}, quantity={}", productId, quantity, exception);
            throw exception;
        }
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock()
        );
    }
}
