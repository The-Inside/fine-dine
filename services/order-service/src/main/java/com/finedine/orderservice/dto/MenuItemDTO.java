package com.finedine.orderservice.dto;

import java.math.BigDecimal;

public record MenuItemDTO(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String category,
        Boolean isAvailable,
        Long restaurantId
) {}