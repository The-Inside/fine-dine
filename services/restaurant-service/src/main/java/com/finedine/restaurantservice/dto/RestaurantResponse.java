package com.finedine.restaurantservice.dto;

public record RestaurantResponse(
        Long restaurantId,
        String externalId,
        String email,
        String restaurantName,
        String restaurantCode,
        String phone,
        String logoUrl,
        String address,
        Double latitude,
        Double longitude,
        String cuisine,
        String description,
        Integer travelTimeMinutes,
        boolean isOpen
) {}
