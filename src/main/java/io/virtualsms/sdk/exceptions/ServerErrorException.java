package io.virtualsms.sdk.exceptions;

/**
 * Thrown on a 5xx response. On a GET/HEAD, the SDK's own bounded retry logic
 * (3 attempts, exponential backoff) already tried before this surfaced, so a
 * thrown instance here is safe-to-retry-yourself. On a mutating call
 * (POST/PUT/PATCH/DELETE), {@link #isMutatingRequest()} is true and the
 * operation may have completed server-side despite the error — the SDK never
 * auto-retries a mutating call, and neither should you without first
 * verifying via a read call (list_orders/get_order/list_rentals/etc.).
 */
public class ServerErrorException extends VirtualSmsException {

    private final boolean mutatingRequest;

    public ServerErrorException(String message, boolean mutatingRequest) {
        super(message, 500);
        this.mutatingRequest = mutatingRequest;
    }

    /** True when this error came from a POST/PUT/PATCH/DELETE, not a GET/HEAD. */
    public boolean isMutatingRequest() {
        return mutatingRequest;
    }
}
