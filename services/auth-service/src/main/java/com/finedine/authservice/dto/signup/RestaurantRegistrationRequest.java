package com.finedine.authservice.dto.signup;


import com.finedine.common.validators.ImageFiles;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;

import static com.finedine.authservice.CustomMessages.*;

/**
 * DTO for restaurant registration.
 * This record is used to transfer restaurant registration data in API requests.
 */

public record RestaurantRegistrationRequest(

    @Email
    String email,
    String password,
    @NotBlank(message = NAME_NOT_BLANK)
    String ownerFirstName,
    @NotBlank(message = NAME_NOT_BLANK)
    String ownerLastName,
    @NotBlank(message = RESTAURANT_NAME_NOT_BLANK)
    String restaurantName,
    String phoneNumber,
    @NotBlank(message = ADDRESS_NOT_BLANK)
    String address,
    @NotNull(message = LONGITUDE_NOT_NULL)
    Double longitude,
    @NotNull(message = LATITUDE_NOT_NULL)
    Double latitude,
    String cuisine,
    String description,
    LocalTime openTime,
    LocalTime closeTime,
    @ImageFiles(message = PHOTO_MUST_BE_VALID)
    MultipartFile image
    ){
}
