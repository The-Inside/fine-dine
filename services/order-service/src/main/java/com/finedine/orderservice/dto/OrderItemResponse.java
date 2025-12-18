package com.finedine.orderservice.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderItemResponse(
        Long id,
        Long menuItemId,
        Integer quantity,
        BigDecimal price
) {}
