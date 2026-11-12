package com.chatbox.service;

/**
 * Exception representing errors during AI requests (network timeouts,
 * invalid credentials, rate limits, or server faults) with user-friendly messages.
 */
public class AiException extends RuntimeException {
    public enum ErrorType {
        AUTHENTICATION_FAILED,
        RATE_LIMITED,
        TIMEOUT,
        MODEL_NOT_FOUND,
        SERVER_ERROR,
        NETWORK_UNREACHABLE,
        INVALID_RESPONSE,
        UNKNOWN
    }

    private final ErrorType errorType;
    private final int httpStatusCode;

    public AiException(String message, ErrorType errorType, int httpStatusCode) {
        super(message);
        this.errorType = errorType;
        this.httpStatusCode = httpStatusCode;
    }

    public AiException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.httpStatusCode = 0;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
