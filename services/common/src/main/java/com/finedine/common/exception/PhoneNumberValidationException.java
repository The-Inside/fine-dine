package com.finedine.common.exception;

public class PhoneNumberValidationException extends IllegalArgumentException{
    public PhoneNumberValidationException(String message) {
        super(message);
    }

    public PhoneNumberValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
