package com.finedine.common;

/**
 * A utility class that holds custom message constants used throughout the application.
 * This class provides a centralized location for defining and managing various message strings,
 * such as error messages, success messages, and validation messages.
 */

public class CustomMessages {
    public static final String EMAIL_NOT_BLANK = "Email cannot be blank";
    public static final String NAME_NOT_BLANK = "Name cannot be blank";
    public static final String PHONE_NUMBER_NOT_BLANK = "Phone number cannot be empty";
    public static final String INVALID_PHONE_NUMBER = "Invalid phone number";
    public static final String PASSWORD_NOT_BLANK = "Password cannot be blank";

    public static final String PHOTO_MUST_BE_VALID = "Photo must be a valid image file (jpg, jpeg, png, gif)";
    public static final String VERIFICATION_PROCESS_FAILED = "The verification process failed. Please check the provided information.";
    public static final String AUTHENTICATION_FAILED_ERROR_MSG = "Incorrect email or password";
    public static final String INTERNAL_SERVER_ERROR_MSG = "An unexpected error occurred. Please try again later.";
    public static final String UNAUTHORIZED_ACCESS_MSG = "You are not authorized to access this resource.";
    public static final String RESOURCE_NOT_FOUND_MSG = "The requested resource could not be found.";
    public static final String BAD_REQUEST_MSG = "The request could not be understood or was missing required parameters.";
    public static final String RATE_LIMIT_EXCEEDED_MSG = "You have exceeded the allowed number of requests. Please try again later.";
    public static final String ACCOUNT_VERIFICATION_REQUIRED = "Account verification is required to access this resource";
    public static final String VALIDATION_ERROR_MSG = "One or more validation errors occurred. Please check the input data.";
}
