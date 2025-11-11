package com.finedine.riderservice.dto;

public record GeofenceAlert(
    String alertType,
    double distanceKm,
    String message
) {}
