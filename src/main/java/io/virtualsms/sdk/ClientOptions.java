package io.virtualsms.sdk;

/**
 * Optional constructor settings for {@link VirtualSmsClient}.
 * <p>
 * Usage: {@code new VirtualSmsClient(apiKey, ClientOptions.builder().baseUrl("https://virtualsms.io/api/v1").build())}
 * or simply {@code new VirtualSmsClient(apiKey)} to accept every default.
 */
public final class ClientOptions {

    /** Default base URL. Overridable via this options object or the {@code VIRTUALSMS_BASE_URL} env var. */
    public static final String DEFAULT_BASE_URL = "https://virtualsms.io/api/v1";

    /** Default request timeout, matching the shared SDK contract (client.ts timeoutSeconds ?? 30). */
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final String baseUrl;
    private final int timeoutSeconds;

    private ClientOptions(Builder builder) {
        String envBaseUrl = System.getenv("VIRTUALSMS_BASE_URL");
        this.baseUrl = builder.baseUrl != null ? builder.baseUrl
                : (envBaseUrl != null && !envBaseUrl.isBlank() ? envBaseUrl : DEFAULT_BASE_URL);
        this.timeoutSeconds = builder.timeoutSeconds != null ? builder.timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public static ClientOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String baseUrl;
        private Integer timeoutSeconds;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public ClientOptions build() {
            return new ClientOptions(this);
        }
    }
}
