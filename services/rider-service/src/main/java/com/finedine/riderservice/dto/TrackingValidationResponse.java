package com.finedine.riderservice.dto;

public record TrackingValidationResponse(
    Long riderId,
    Long deliveryId,
    String riderName,
    String vehicleType,
    String subscriptionTopic
) {}
