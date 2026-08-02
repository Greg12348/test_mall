package com.libo.mall.order.controller;

import com.libo.mall.order.dto.CreateOrderRequest;
import com.libo.mall.order.dto.OrderResponse;
import com.libo.mall.order.entity.OrderStatus;
import com.libo.mall.order.exception.OrderNotFoundException;
import com.libo.mall.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldCreateOrder() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 31, 12, 0);
        OrderResponse order = new OrderResponse(
                10L,
                1L,
                2L,
                3,
                new BigDecimal("75.00"),
                OrderStatus.PENDING,
                createdAt,
                createdAt
        );

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "productId": 2,
                                  "quantity": 3,
                                  "totalAmount": 75.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Order created successfully"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.productId").value(2))
                .andExpect(jsonPath("$.data.quantity").value(3))
                .andExpect(jsonPath("$.data.totalAmount").value(75.00))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void shouldReturnOrderById() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 31, 12, 0);
        OrderResponse order = new OrderResponse(
                10L,
                1L,
                2L,
                3,
                new BigDecimal("75.00"),
                OrderStatus.STOCK_RESERVED,
                createdAt,
                createdAt.plusSeconds(1)
        );

        when(orderService.getOrderById(10L)).thenReturn(order);

        mockMvc.perform(get("/orders/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Order retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.totalAmount").value(75.00))
                .andExpect(jsonPath("$.data.status").value("STOCK_RESERVED"));
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderService.getOrderById(99L)).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found with id: 99"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
