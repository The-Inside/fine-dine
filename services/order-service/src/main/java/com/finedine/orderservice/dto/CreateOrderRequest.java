package com.finedine.orderservice.dto;

import java.util.List;

public record CreateOrderRequest(
        Long restaurantId,
        List<OrderItemRequest> orderItems
) {}
