package com.finedine.authservice.dto;

import com.finedine.common.validators.ImageFiles;
import org.springframework.web.multipart.MultipartFile;

import static com.finedine.authservice.CustomMessages.PHOTO_MUST_BE_VALID;

/**
 * DTO for uploading a profile picture.
 *
 * @param profilePicture the profile picture file to be uploaded
 */
public record ProfilePictureUpload(
        @ImageFiles(message = PHOTO_MUST_BE_VALID)
        MultipartFile profilePicture
) {
}
