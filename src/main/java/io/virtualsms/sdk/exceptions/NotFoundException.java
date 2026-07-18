package io.virtualsms.sdk.exceptions;

/** Thrown on HTTP 404 — order/rental/proxy/webhook id (or route) not found. */
public class NotFoundException extends VirtualSmsException {
    public NotFoundException(String message) {
        super(message, 404);
    }
}
