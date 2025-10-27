package com.finedine.customerservice.dto;

import lombok.Builder;

@Builder
public record OrderResponse(
        String orderId,
        String foodItem
) {
}