package io.virtualsms.sdk.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualsms.sdk.exceptions.ApiException;
import io.virtualsms.sdk.exceptions.BadApiKeyException;
import io.virtualsms.sdk.exceptions.InsufficientBalanceException;
import io.virtualsms.sdk.exceptions.NoNumbersException;
import io.virtualsms.sdk.exceptions.NotFoundException;
import io.virtualsms.sdk.exceptions.RateLimitedException;
import io.virtualsms.sdk.exceptions.ServerErrorException;
import io.virtualsms.sdk.exceptions.VirtualSmsException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thin HTTP layer over {@link HttpClient}: attaches {@code X-API-Key} and a
 * fresh {@code X-Idempotency-Key} on every mutating request, runs the
 * GET-only bounded retry policy (3 total attempts, exponential backoff,
 * retries on no-response or 5xx only — never on 4xx), and maps non-2xx
 * responses to the typed exception hierarchy. Mirrors the MCP server's
 * axios interceptor (client.ts) byte-for-byte in behavior.
 */
public final class HttpExecutor {

    /** 1 initial try + up to 2 retries, GET/HEAD only. */
    public static final int GET_RETRY_MAX_ATTEMPTS = 3;
    private static final long GET_RETRY_BASE_DELAY_MS = 300;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final Duration timeout;

    public HttpExecutor(String baseUrl, String apiKey, int timeoutSeconds) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
    }

    /** Whether a failed GET/HEAD should be retried, given attempts made so far (including the failed one). */
    public static boolean shouldRetryGet(String method, Integer status, boolean hasResponse, int attemptsSoFar) {
        String m = method.toLowerCase();
        if (!m.equals("get") && !m.equals("head")) return false;
        if (attemptsSoFar >= GET_RETRY_MAX_ATTEMPTS) return false;
        if (!hasResponse) return true;
        return status != null && status >= 500;
    }

    /** Exponential backoff delay before retry attempt number {@code attemptNumber} (1-indexed). */
    public static long getRetryDelayMs(int attemptNumber) {
        return GET_RETRY_BASE_DELAY_MS * (1L << (attemptNumber - 1));
    }

    public JsonNode get(String path, Map<String, ?> query) {
        return execute("GET", path, query, null);
    }

    public JsonNode post(String path, Object body) {
        return execute("POST", path, null, body);
    }

    public JsonNode patch(String path, Object body) {
        return execute("PATCH", path, null, body);
    }

    public JsonNode delete(String path) {
        return execute("DELETE", path, null, null);
    }

    private JsonNode execute(String method, String path, Map<String, ?> query, Object body) {
        String url = baseUrl + path + buildQueryString(query);
        String requestBody = body != null ? toJson(body) : "";
        boolean mutating = !method.equals("GET") && !method.equals("HEAD");

        int attempt = 0;
        while (true) {
            attempt++;
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("X-API-Key", apiKey);
            }
            if (mutating) {
                builder.header("X-Idempotency-Key", UUID.randomUUID().toString());
            }
            switch (method) {
                case "GET" -> builder.GET();
                case "DELETE" -> builder.DELETE();
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));
                case "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));
                default -> throw new IllegalArgumentException("Unsupported method: " + method);
            }

            HttpResponse<String> response;
            boolean hasResponse;
            Integer status = null;
            try {
                response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                hasResponse = true;
                status = response.statusCode();
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                if (shouldRetryGet(method, null, false, attempt)) {
                    sleepQuietly(getRetryDelayMs(attempt));
                    continue;
                }
                throw new VirtualSmsException("Network error calling VirtualSMS API: " + e.getMessage(), e);
            }

            if (status >= 200 && status < 300) {
                return parseJson(response.body());
            }

            if (shouldRetryGet(method, status, hasResponse, attempt)) {
                sleepQuietly(getRetryDelayMs(attempt));
                continue;
            }

            throw mapError(status, response.body(), mutating);
        }
    }

    private VirtualSmsException mapError(int status, String rawBody, boolean isMutating) {
        String message = extractMessage(rawBody);
        // No-supplier-name rule applies to error messages too: the API never
        // returns a supplier name in error bodies, so no scrubbing needed here,
        // but nothing in this SDK ever logs a supplier name either.
        switch (status) {
            case 401:
                return new BadApiKeyException("Invalid API key. Get one at https://virtualsms.io");
            case 402:
                return new InsufficientBalanceException("Insufficient balance. Top up at https://virtualsms.io");
            case 404:
                return new NotFoundException("Not found: " + message);
            case 429:
                return new RateLimitedException("Rate limit exceeded. Please slow down requests.");
            default:
                if (status >= 500) {
                    String lowerMessage = message.toLowerCase();
                    if (lowerMessage.contains("out of stock") || lowerMessage.contains("no numbers")) {
                        return new NoNumbersException("No numbers currently available: " + message, isMutating);
                    }
                    String msg = isMutating
                            ? "VirtualSMS had a server error (" + status + ") on a request that may have made a " +
                              "purchase or changed state. DO NOT blindly retry: first verify with a read call " +
                              "(list_orders, get_order, list_rentals, etc.) whether it actually succeeded, as you " +
                              "may have been charged. Details: " + message
                            : "VirtualSMS server error (" + status + "). Safe to retry this read-only request. Details: " + message;
                    return new ServerErrorException(msg, isMutating);
                }
                return new ApiException("API error: " + message, status);
        }
    }

    private String extractMessage(String rawBody) {
        try {
            JsonNode node = parseJson(rawBody);
            if (node != null) {
                if (node.hasNonNull("message")) return node.get("message").asText();
                if (node.hasNonNull("error")) return node.get("error").asText();
            }
        } catch (RuntimeException ignored) {
            // Fall through to raw body below.
        }
        return rawBody == null || rawBody.isBlank() ? "unknown error" : rawBody;
    }

    private JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            return Json.MAPPER.readTree(body);
        } catch (IOException e) {
            throw new VirtualSmsException("Failed to parse VirtualSMS API response: " + e.getMessage(), e);
        }
    }

    private String toJson(Object body) {
        try {
            return Json.MAPPER.writeValueAsString(body);
        } catch (IOException e) {
            throw new VirtualSmsException("Failed to serialize request body: " + e.getMessage(), e);
        }
    }

    private String buildQueryString(Map<String, ?> query) {
        if (query == null || query.isEmpty()) return "";
        Map<String, String> present = new LinkedHashMap<>();
        for (Map.Entry<String, ?> e : query.entrySet()) {
            if (e.getValue() != null) present.put(e.getKey(), String.valueOf(e.getValue()));
        }
        if (present.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String> e : present.entrySet()) {
            if (!first) sb.append('&');
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
