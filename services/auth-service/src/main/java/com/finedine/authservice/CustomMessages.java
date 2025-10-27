package com.finedine.authservice;

/**
 * A utility class that holds custom message constants used throughout the application.
 * This class provides a centralized location for defining and managing various message strings,
 * such as error messages, success messages, and validation messages.
 */

public class CustomMessages {
    public static final String EMAIL_NOT_BLANK = "Email cannot be blank";
    public static final String ADDRESS_NOT_BLANK = "Address cannot be empty";
    public static final String NAME_NOT_BLANK = "Name cannot be blank";
    public static final String RESTAURANT_NAME_NOT_BLANK = "Restaurant name cannot be blank";
    public static final String PHONE_NUMBER_NOT_BLANK = "Phone number cannot be empty";
    public static final String INVALID_PHONE_NUMBER = "Invalid phone number";
    public static final String PASSWORD_NOT_BLANK = "Password cannot be blank";
    public static final String LONGITUDE_NOT_NULL = "Longitude cannot be null";
    public static final String LATITUDE_NOT_NULL = "Latitude cannot be null";
    public static final String USER_NOT_FOUND = "No such user";
    public static final String VERIFICATION_FAILED = "Something went wrong, could not send email";
    public static final String EMAIL_ALREADY_EXISTS = "An account with the provided email already exists";
    public static final String OTP_VERIFICATION_FAILED_MESSAGE = "Could not verify this OTP";
    public static final String EMAIL_MISMATCH_MESSAGE = "Could not verify email for this OTP";
    public static final String OTP_EXPIRED_MESSAGE = "OTP expired";
    public static final String RESET_PASSWORD_SUCC = "Password Reset Successfully, login in";
    public static final String OTP_NOT_VERIFIED = "OTP could not be verified, may be expired or invalid";

    private static final String JWT_EXC_MSG = "Could not authenticate you, try again";

    public static final String TOKEN_SENT_MSG = "Token Sent, check your email";
    public static final String PROFILE_UPDATE_SUCCESS = "Profile updates successful";
    public static final String PASSWORD_MISMATCH = "Passwords do not match";

    public static final String GENERIC_NOT_FOUND_MSG = "Resource not found";
    public static final String BAD_CREDENTIALS_MESSAGE = "Invalid email or password";
    public static final String USER_NOT_VERIFIED_MSG = "Unverified user";
    public static final String INVALID_REQUEST_MSG = "Invalid request, please try again";
    public static final String INVALID_EMAIL_OR_PASSWORD_MSG = "Please check your password or email format and please try again";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error occurred";
    public static final String RESTRICTED_ACTION = "You do not have permission to perform this action. Please contact your administrator if you believe this is an error.";
    public static final String ACCOUNT_DELETED_SUCCESS = "Account deleted successfully";
    public static final String PHOTO_MUST_BE_VALID = "Photo must be a valid image file (jpg, jpeg, png, gif)";
    public static final String PROFILE_PHOTO_MUST_BE_VALID = "Profile picture must be a valid image file (jpg, jpeg, png, gif)";
    public static final String KEYS_ROTATED_SUCCESSFULLY = "Keys rotated successfully";
    public static final String ACCOUNT_CREATED_SUCCESS = "Account created successfully";
    public static final String UNVERIFIED_ACCOUNT = "Account is not verified yet";
    public static final String ACCOUNT_VERIFIED_SUCCESSFULLY = "Account verified successfully";
    public static final String ACCOUNT_VERIFICATION_FAILED = "Account verification failed";
    public static final String ACCOUNT_DISABLED = "Account is disabled. Please contact support for assistance.";

}
