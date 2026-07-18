package io.virtualsms.sdk.exceptions;

/**
 * Base exception for every error the VirtualSMS SDK can throw. All typed
 * subclasses below extend this, so callers can catch broadly or narrowly.
 */
public class VirtualSmsException extends RuntimeException {

    private final Integer statusCode;

    public VirtualSmsException(String message) {
        super(message);
        this.statusCode = null;
    }

    public VirtualSmsException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public VirtualSmsException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    /** HTTP status code that produced this error, if known. */
    public Integer getStatusCode() {
        return statusCode;
    }
}
