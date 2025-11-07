package com.finedine.common.exception;

/**
 * Exception thrown when a rate limit is exceeded.
 * This exception is handled by the GlobalExceptionHandler to return a 429 response.
 */

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }

}