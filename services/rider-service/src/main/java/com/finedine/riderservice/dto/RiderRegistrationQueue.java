package com.finedine.riderservice.dto;

import com.finedine.riderservice.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;



/**
 * DTO for rider registration queue.
 * This record is used to transfer rider registration data in asynchronous processing between auth and rider services.
 */

@Builder
public record RiderRegistrationQueue(
        @NotNull
        Long accountId,

        @NotBlank
        String email,

        @NotBlank
        String externalId,

        @NotBlank
        String address,

        @NotBlank
        String phoneNumber,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        VehicleType vehicleType,

        String licensePlateNumber,

        String vehicleColor,

        String profilePictureUrl
) {
}
