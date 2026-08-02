package com.libo.mall.order.service;

import com.libo.mall.order.dto.CreateOrderRequest;
import com.libo.mall.order.dto.OrderResponse;
import com.libo.mall.order.entity.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(Long id);

    List<OrderResponse> getAllOrders();

    List<OrderResponse> getOrdersByUserId(Long userId);

    OrderResponse updateOrderStatus(Long id, OrderStatus status);

    OrderResponse payOrder(Long id);

    OrderResponse cancelOrder(Long id);

    OrderResponse completeOrder(Long id);

    OrderResponse getOrderDetail(Long id);
}
