package com.finedine.common.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Validator class for the ImageFiles annotation.
 * This class implements the validation logic to check if the provided files are valid image files.
 * It supports both single MultipartFile and an array of MultipartFile objects.
 */

public class ImageFilesValidator implements ConstraintValidator<ImageFiles, Object> {
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif");

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        if (value instanceof MultipartFile file) {
            return validateSingleFile(file);
        } else if (value instanceof MultipartFile[] files) {
            if (files.length == 0) {
                return false;
            }
            for (MultipartFile file : files) {
                if (!validateSingleFile(file)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean validateSingleFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (originalFilename == null || contentType == null) {
            return false;
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension) && ALLOWED_MIME_TYPES.contains(contentType);
    }
}