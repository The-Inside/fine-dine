package com.finedine.riderservice.dto;

public record RiderLocationResponse(
    Long riderId,
    double latitude,
    double longitude,
    long timestamp,
    Double etaMinutes,
    String etaText
) {}
