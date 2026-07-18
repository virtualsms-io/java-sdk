package io.virtualsms.sdk.exceptions;

/** Generic fallback for any other 4xx status not covered by a dedicated subclass. */
public class ApiException extends VirtualSmsException {
    public ApiException(String message, int statusCode) {
        super(message, statusCode);
    }
}
