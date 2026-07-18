package io.virtualsms.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Shared/account/catalog response types used across multiple method groups.
 * Grouped as nested classes to keep the model package manageable; each is a
 * plain data-holder deserialized directly from JSON by Jackson (field
 * visibility, unknown properties ignored so the SDK doesn't break when the
 * API adds new fields).
 */
public final class Common {

    private Common() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Service {
        public String code;
        public String name;
        public String icon;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Country {
        public String iso;
        public String name;
        public String flag;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        public double priceUsd;
        public String currency;
        public boolean available;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogCountry {
        public String iso;
        public String name;
        public double priceUsd;
        public int count;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Balance {
        public double balanceUsd;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        public String id;
        public String email;
        public boolean telegramLinked;
        public String telegramUsername;
        public double balanceUsd;
        public double totalSpentUsd;
        public double totalCreditsUsd;
        public int totalOrders;
        public int activeApiKeys;
        public String createdAt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transaction {
        public String id;
        public double amount;
        public String type;
        public String description;
        public String orderId;
        public double balanceBefore;
        public double balanceAfter;
        public String createdAt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionsPage {
        public int count;
        public int limit;
        public int offset;
        public List<Transaction> transactions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SmsMessage {
        public String content;
        public String sender;
        public String receivedAt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NumberCheckResult {
        public boolean valid;
        public String e164;
        public String national;
        public String countryCode;
        public String countryName;
        public String countryPrefix;
        public String location;
        public String carrier;
        public String lineType;
        public String spamRisk;
        public boolean cached;
        public String message;
    }
}
