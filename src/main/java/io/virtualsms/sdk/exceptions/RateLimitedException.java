package io.virtualsms.sdk.exceptions;

/**
 * Thrown on HTTP 429 — rate limit exceeded. Never auto-retried by the SDK's
 * GET-retry logic (fighting the server's own rate limiter is wrong); slow
 * down and retry later.
 */
public class RateLimitedException extends VirtualSmsException {
    public RateLimitedException(String message) {
        super(message, 429);
    }
}
