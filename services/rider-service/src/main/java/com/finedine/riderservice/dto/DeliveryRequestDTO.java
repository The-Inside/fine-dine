package com.finedine.riderservice.dto;


import lombok.Builder;

@Builder
public record DeliveryRequestDTO(
        Long orderId,
        Long restaurantId,
        double restaurantLat,
        double restaurantLon,
        double customerLat,
        double customerLon
) {}