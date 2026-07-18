package io.virtualsms.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Activation/order response types, including the client-side helper results
 * (get_sms, wait_for_sms, order_history, cancel_all_orders, search_services,
 * find_cheapest, get_stats) that have no dedicated backend route and are
 * assembled locally from other calls, mirroring the MCP tool handlers.
 */
public final class Orders {

    private Orders() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rules {
        public Integer cancelCooldownSeconds;
        public Integer swapCooldownSeconds;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Order {
        public String orderId;
        public String phoneNumber;
        public String service;
        public String country;
        public Double price;
        public String createdAt;
        public String expiresAt;
        public String status;
        // Legacy fields, kept for backward compat with older API responses.
        public String smsCode;
        public String smsText;
        // Canonical SMS payload: one entry per inbound message.
        public List<Common.SmsMessage> messages;
        public Boolean smsReceived;
        // Cooldown timestamps: RFC3339 wallclock when cancel/swap become available.
        public String cancelAvailableAt;
        public String swapAvailableAt;
        public Rules rules;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CancelResult {
        public boolean success;
        public boolean refunded;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RetryOrderResult {
        public boolean success;
        public String orderId;
        public String message;
    }

    /** Result of the client-side get_sms helper (thin normalizer over get_order). */
    public static class GetSmsResult {
        public String status;
        public String phoneNumber;
        public List<Common.SmsMessage> messages;
        public String code;
        public String smsCode;
        public String smsText;
    }

    /** Result of the client-side wait_for_sms helper. */
    public static class WaitForSmsResult {
        public boolean success;
        public String orderId;
        public String phoneNumber;
        public String status;
        public List<Common.SmsMessage> messages;
        public String code;
        public String smsCode;
        public String smsText;
        public String deliveryMethod;
        public Long elapsedSeconds;
        public Integer pollAttempts;
        /** Only set when success == false (timeout). */
        public String error;
        public String message;
        public String tip;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderHistoryEntry {
        public String orderId;
        public String phoneNumber;
        public String service;
        public String country;
        public Double price;
        public String status;
        public String createdAt;
        public String smsCode;
    }

    public static class OrderHistoryFilters {
        public String status;
        public String service;
        public String country;
        public Integer sinceDays;
    }

    public static class OrderHistoryResult {
        public int count;
        public int totalMatched;
        public OrderHistoryFilters filters;
        public List<OrderHistoryEntry> orders;
    }

    public static class CancelledOrderEntry {
        public String orderId;
        public boolean refunded;
    }

    public static class CancelFailureEntry {
        public String orderId;
        public String error;
    }

    public static class CancelAllOrdersResult {
        public int cancelled;
        public int failed;
        public int totalActive;
        public List<CancelledOrderEntry> cancelledOrders;
        public List<CancelFailureEntry> failures;
        public String message;
    }

    public static class ServiceMatch {
        public String code;
        public String name;
        public double matchScore;
    }

    public static class SearchServicesResult {
        public String query;
        public List<ServiceMatch> matches;
        public String message;
        public String tip;
    }

    public static class CheapestOption {
        public String country;
        public String countryName;
        public double priceUsd;
        public boolean stock;
    }

    public static class FindCheapestResult {
        public String service;
        public List<CheapestOption> cheapestOptions;
        public int totalAvailableCountries;
        public String message;
    }

    public static class StatsResult {
        public int windowDays;
        public double balanceUsd;
        public int totalOrders;
        public int successfulOrders;
        public Double successRate;
        public double totalSpendUsd;
        public Map<String, Integer> statusBreakdown;
        public List<KeyCount> topServices;
        public List<KeyCount> topCountries;
        public String note;
    }

    public static class KeyCount {
        public String key;
        public int count;
    }
}
