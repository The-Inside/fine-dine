package com.finedine.riderservice.dto;

import lombok.Builder;

@Builder
public record RiderResponse(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String licensePlateNumber,
        String vehicleType,
        String status,
        String profilePictureUrl
) {
}
