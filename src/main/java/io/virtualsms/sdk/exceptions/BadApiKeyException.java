package io.virtualsms.sdk.exceptions;

/** Thrown on HTTP 401 — invalid or missing API key. */
public class BadApiKeyException extends VirtualSmsException {
    public BadApiKeyException(String message) {
        super(message, 401);
    }
}
