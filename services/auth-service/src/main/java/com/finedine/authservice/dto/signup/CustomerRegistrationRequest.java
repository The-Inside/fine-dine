package com.finedine.authservice.dto.signup;

//import com.finedine.authservice.util.contraints.ImageFiles;
import com.finedine.common.validators.ImageFiles;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import static com.finedine.authservice.CustomMessages.*;


/**
 * DTO for customer registration.
 * This record is used to transfer customer registration data in API requests.
 */

@Builder
public record CustomerRegistrationRequest(
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
        String address,
        @ImageFiles(message = PHOTO_MUST_BE_VALID)
        MultipartFile image
) {
}
