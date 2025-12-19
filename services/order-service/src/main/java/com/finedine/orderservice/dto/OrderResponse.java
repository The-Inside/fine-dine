package com.finedine.orderservice.dto;

import com.finedine.orderservice.enums.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderResponse(
        Long id,
        String customerExternalId,
        Long restaurantId,
        Long riderId,
        BigDecimal totalAmount,
        OrderStatus orderStatus,
        List<OrderItemResponse> orderItems,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
