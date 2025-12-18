package com.finedine.orderservice.dto;

public record OrderItemRequest(
        Long menuItemId,
        Integer quantity
) {}
