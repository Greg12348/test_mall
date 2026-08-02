package com.libo.mall.order.controller;

import com.libo.mall.order.dto.ApiResponse;
import com.libo.mall.order.dto.CreateOrderRequest;
import com.libo.mall.order.dto.OrderResponse;
import com.libo.mall.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id);
        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .data(order)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestBody CreateOrderRequest request
    ) {
        OrderResponse order = orderService.createOrder(request);
        HttpStatus status = HttpStatus.CREATED;
        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .status(status.value())
                .message("Order created successfully")
                .data(order)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
