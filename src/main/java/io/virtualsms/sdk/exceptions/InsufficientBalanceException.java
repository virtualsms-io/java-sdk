package io.virtualsms.sdk.exceptions;

/** Thrown on HTTP 402 — account balance too low for the requested purchase. */
public class InsufficientBalanceException extends VirtualSmsException {
    public InsufficientBalanceException(String message) {
        super(message, 402);
    }
}
