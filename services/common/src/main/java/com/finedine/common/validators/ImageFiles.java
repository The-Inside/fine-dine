package com.finedine.common.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for validating image files.
 * This annotation can be applied to fields to ensure that they meet the criteria for valid image files.
 * The actual validation logic is implemented in the ImageFilesValidator class.
 */

@Constraint(validatedBy = ImageFilesValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageFiles {
    String message();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
