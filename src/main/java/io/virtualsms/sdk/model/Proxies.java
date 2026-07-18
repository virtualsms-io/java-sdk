package io.virtualsms.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Proxy catalog / owned-proxy / usage / targeting / test / endpoint response types. */
public final class Proxies {

    private Proxies() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyCatalogCountry {
        public String code;
        public String name;
        public boolean available;
        public int ipCount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyCatalogPoolType {
        public String id;
        public String label;
        public double pricePerGb;
        public List<ProxyCatalogCountry> countries;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyListItem {
        public String proxyId;
        public String poolType;
        public String countryCode;
        public String countryName;
        public double gbTotal;
        public double gbUsed;
        public double gbRemaining;
        public String proxyHost;
        public int proxyPort;
        public String proxyLogin;
        public String proxyPassword;
        public String updatedAt;
        public String createdAt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyPurchaseResult {
        public String proxyId;
        public String poolType;
        public double gbAdded;
        public double gbRemaining;
        public String countryCode;
        public String proxyLogin;
        public String proxyPassword;
        public String proxyHost;
        public int proxyPort;
        public Integer proxyPortSocks;
        public double price;
        public Double balance;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyRotateResult {
        public boolean rotated;
        public int port;
        public String message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyUsage {
        public double gbUsed;
        public double gbRemaining;
        public long requests;
        public String updatedAt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyUsageHistoryPoint {
        public String date;
        public double gb;
        public long requests;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyUsageHistoryTotals {
        public double gb;
        public long requests;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyUsageHistoryResult {
        public List<ProxyUsageHistoryPoint> series;
        public ProxyUsageHistoryTotals totals;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyTargetingResult {
        public boolean ok;
        public String countryCode;
        /**
         * True when city/state/zip/asn targeting was requested on a non-premium
         * pool: the sub-country refinement burns funded GB 2x faster. Free on
         * residential_premium.
         * <p>
         * Explicit {@code @JsonProperty} because Jackson's SNAKE_CASE naming
         * strategy only splits on lower-to-uppercase transitions, so the
         * camelCase field {@code premium2x} would serialize to {@code
         * premium2x}, not the API's actual {@code premium_2x}.
         */
        @JsonProperty("premium_2x")
        public boolean premium2x;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyTestResult {
        public boolean ok;
        public String exitIp;
        public String countryCode;
        public String countryName;
        public String city;
        public String region;
        public String isp;
        public String asn;
        public Long latencyMs;
        public String error;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProxyLocationItem {
        public String code;
        public String name;
        public int count;
    }

    public static class ProxyEndpointResult {
        public String proxyId;
        public String poolType;
        public String host;
        public int port;
        public String protocol;
        public String session;
        public Integer stickyTtlMinutes;
        public String countryCode;
        public String targetBy;
        public String locationCode;
        @JsonProperty("premium_2x")
        public boolean premium2x;
        public List<String> endpoints;
    }
}
