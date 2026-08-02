package com.libo.mall.order.service.impl;

import com.libo.mall.order.client.ProductClient;
import com.libo.mall.order.client.dto.ProductResponse;
import com.libo.mall.order.dto.CreateOrderRequest;
import com.libo.mall.order.dto.OrderResponse;
import com.libo.mall.order.entity.OrderStatus;
import com.libo.mall.order.entity.Order;
import com.libo.mall.order.exception.OrderNotFoundException;
import com.libo.mall.order.repository.OrderRepository;
import com.libo.mall.order.outbox.OrderOutboxService;
import com.libo.mall.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderOutboxService orderOutboxService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductClient productClient,
            OrderOutboxService orderOutboxService
    ) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.orderOutboxService = orderOutboxService;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order. userId={}, productId={}, quantity={}",
                request.userId(), request.productId(), request.quantity());
        try {
            // TODO: Call user-service to check whether request.userId() exists.
            ProductResponse product = productClient.getProductById(request.productId()).getData();

            LocalDateTime now = LocalDateTime.now();
            Order order = Order.builder()
                    .userId(request.userId())
                    .productId(request.productId())
                    .quantity(request.quantity())
                    .totalAmount(product.price().multiply(BigDecimal.valueOf(request.quantity())))
                    .status(OrderStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Order savedOrder = orderRepository.save(order);
            orderOutboxService.saveOrderCreated(savedOrder);
            OrderResponse response = toResponse(savedOrder);
            log.info("Created order successfully. orderId={}, userId={}, productId={}",
                    response.id(), response.userId(), response.productId());
            return response;
        } catch (RuntimeException exception) {
            log.error("Failed to create order. userId={}, productId={}, quantity={}",
                    request.userId(), request.productId(), request.quantity(), exception);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Getting order by id. orderId={}", id);
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new OrderNotFoundException(id));
            OrderResponse response = toResponse(order);
            log.info("Got order successfully. orderId={}, status={}", id, response.status());
            return response;
        } catch (RuntimeException exception) {
            log.error("Failed to get order by id. orderId={}", id, exception);
            throw exception;
        }
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        log.info("Getting all orders");
        try {
            // TODO: Query all orders from the order database.
            // TODO: Convert order entities to order responses.
            log.info("getAllOrders is not implemented yet");
            return List.of();
        } catch (RuntimeException exception) {
            log.error("Failed to get all orders", exception);
            throw exception;
        }
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.info("Getting orders by user id. userId={}", userId);
        try {
            // TODO: Call user-service to check whether userId exists.
            // TODO: Query all orders that belong to this user.
            // TODO: Convert order entities to order responses.
            log.info("getOrdersByUserId is not implemented yet. userId={}", userId);
            return List.of();
        } catch (RuntimeException exception) {
            log.error("Failed to get orders by user id. userId={}", userId, exception);
            throw exception;
        }
    }

    @Override
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        log.info("Updating order status. orderId={}, status={}", id, status);
        try {
            // TODO: Find order by id from the order database.
            // TODO: Change the order status.
            // TODO: Update the updatedAt time.
            // TODO: Save and return the updated order response.
            log.info("updateOrderStatus is not implemented yet. orderId={}, status={}", id, status);
            return null;
        } catch (RuntimeException exception) {
            log.error("Failed to update order status. orderId={}, status={}", id, status, exception);
            throw exception;
        }
    }

    @Override
    public OrderResponse payOrder(Long id) {
        log.info("Paying order. orderId={}", id);
        try {
            // TODO: Find order by id from the order database.
            // TODO: Call payment-service to create or confirm payment for this order.
            // TODO: Change order status to PAID after payment succeeds.
            // TODO: Save and return the updated order response.
            log.info("payOrder is not implemented yet. orderId={}", id);
            return null;
        } catch (RuntimeException exception) {
            log.error("Failed to pay order. orderId={}", id, exception);
            throw exception;
        }
    }

    @Override
    public OrderResponse cancelOrder(Long id) {
        log.info("Cancelling order. orderId={}", id);
        try {
            // TODO: Find order by id from the order database.
            // TODO: Call product-service to release stock back to inventory.
            // TODO: Call payment-service to cancel or refund payment if needed.
            // TODO: Change order status to CANCELLED.
            // TODO: Save and return the updated order response.
            log.info("cancelOrder is not implemented yet. orderId={}", id);
            return null;
        } catch (RuntimeException exception) {
            log.error("Failed to cancel order. orderId={}", id, exception);
            throw exception;
        }
    }

    @Override
    public OrderResponse completeOrder(Long id) {
        log.info("Completing order. orderId={}", id);
        try {
            // TODO: Find order by id from the order database.
            // TODO: Call payment-service to confirm the order has been paid.
            // TODO: Change order status to COMPLETED.
            // TODO: Save and return the updated order response.
            log.info("completeOrder is not implemented yet. orderId={}", id);
            return null;
        } catch (RuntimeException exception) {
            log.error("Failed to complete order. orderId={}", id, exception);
            throw exception;
        }
    }

    @Override
    public OrderResponse getOrderDetail(Long id) {
        log.info("Getting order detail. orderId={}", id);
        try {
            // TODO: Find order by id from the order database.
            // TODO: Call user-service to include user details.
            // TODO: Call product-service to include product details.
            // TODO: Call payment-service to include payment details.
            // TODO: Return a detailed order response.
            log.info("getOrderDetail is not implemented yet. orderId={}", id);
            return null;
        } catch (RuntimeException exception) {
            log.error("Failed to get order detail. orderId={}", id, exception);
            throw exception;
        }
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
