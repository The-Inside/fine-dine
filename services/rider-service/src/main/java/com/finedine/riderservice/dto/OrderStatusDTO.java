package com.finedine.riderservice.dto;


import com.finedine.riderservice.enums.DeliveryStatus;
import lombok.Builder;

@Builder
public record OrderStatusDTO (
        Long orderId,
        DeliveryStatus status
) {}
