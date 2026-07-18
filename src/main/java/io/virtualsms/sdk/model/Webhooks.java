package io.virtualsms.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Webhooks (new in v2, not present in the MCP client at all). Base path
 * {@code /api/v1/customer/webhooks}, same {@code X-API-Key} auth as every
 * other customer route.
 */
public final class Webhooks {

    private Webhooks() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookEndpoint {
        public String id;
        public String url;
        public String description;
        public List<String> events;
        public boolean active;
        public boolean paused;
        public Double threshold;
        public int failureCountConsecutive;
        public String lastDeliveredAt;
        public String lastErrorAt;
        public String lastErrorCode;
        public String createdAt;
        public String updatedAt;
        /** Only present on the create-webhook response, exactly once. Store it immediately. */
        public String secret;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListWebhooksResult {
        public boolean success;
        public List<WebhookEndpoint> webhooks;
        public int count;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GetWebhookResult {
        public boolean success;
        public WebhookEndpoint webhook;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeleteWebhookResult {
        public boolean success;
        public String id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TestWebhookResult {
        public boolean success;
        public String message;
        public String deliveryId;
        public String eventId;
        public String eventType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookDelivery {
        public String id;
        public String eventId;
        public String eventType;
        public int attempt;
        public String status;
        public Integer responseStatus;
        public String responseBody;
        public String scheduledFor;
        public String deliveredAt;
        public String errorMessage;
        public String createdAt;
        public Map<String, Object> payload;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListWebhookDeliveriesResult {
        public boolean success;
        public List<WebhookDelivery> deliveries;
        public int count;
        public int limit;
        public int offset;
    }
}
