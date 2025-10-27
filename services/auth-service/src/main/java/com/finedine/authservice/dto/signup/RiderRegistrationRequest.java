package com.finedine.authservice.dto.signup;

import com.finedine.authservice.enums.VehicleType;
import com.finedine.authservice.util.contraints.ImageFiles;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

import static com.finedine.authservice.CustomMessages.*;

/**
 * DTO for registering a new rider.
 * This record is used to transfer rider registration data in API requests.
 */

public record RiderRegistrationRequest(
    @NotBlank(message = EMAIL_NOT_BLANK)
    String email,

    @NotBlank(message = PASSWORD_NOT_BLANK)
    String password,
    @NotBlank(message = NAME_NOT_BLANK)
    String firstName,
    @NotBlank(message = NAME_NOT_BLANK)
    String lastName,
    @NotBlank(message = PHONE_NUMBER_NOT_BLANK)
    String phoneNumber,
    @NotBlank(message = ADDRESS_NOT_BLANK)
    String address,
    VehicleType vehicleType,
    String licensePlateNumber,
    String vehicleColor,
    @ImageFiles(message = PROFILE_PHOTO_MUST_BE_VALID)
    MultipartFile image) {
}
