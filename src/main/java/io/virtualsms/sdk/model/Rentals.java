package io.virtualsms.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Rental response types. Two tiers, both refund-identical (full refund
 * within 20 minutes of purchase and before the first SMS): {@code
 * full_access} (local SIM inventory, any service) and {@code platform}
 * (sourced via our global supplier network, one service per number, 24/72/
 * 168h durations only). Never expose a supplier name anywhere in this SDK.
 */
public final class Rentals {

    private Rentals() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RentalPricingTier {
        public String rentalType;
        public int durationHours;
        public String durationLabel;
        public double basePrice;
        public String countryCode;
        public String serviceId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RentalDurationPrice {
        public int durationHours;
        public String durationLabel;
        public double price;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RentalAvailabilityCountry {
        public String countryCode;
        public String countryName;
        public String flag;
        public int availableCount;
        public Map<String, List<RentalDurationPrice>> pricing;
        /** Populated only for the platform tier (provider=network). */
        public Integer serviceCount;
        public List<String> popularServices;
        public Double minPricePerDay;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RentalFullAccessCountry {
        public String countryCode;
        public String countryName;
        public String flag;
        public int availableCount;
        /** duration_hours (as string key) -> price. */
        public Map<String, Double> pricing;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RentalAvailabilityResult {
        public List<RentalAvailabilityCountry> countries;
        public int totalAvailable;
        public List<RentalFullAccessCountry> fullAccessCountries;
        public String provider;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RentalCatalogService {
        public String serviceId;
        public String serviceName;
        public int physicalCount;
        public Double ourPrice;
        public Double basePrice;
        public boolean popular;
        public String iconUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RentalPriceResult {
        public double price;
        public int durationHours;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rental {
        public String id;
        public String phoneNumber;
        public String rentalType;
        public String serviceId;
        public int durationHours;
        public String startedAt;
        public String expiresAt;
        public double price;
        public boolean autoRenew;
        public String status;
        public int smsReceived;
        public int smsForwarded;
        public String lastSmsAt;
        public String provider;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateRentalResult {
        public boolean success;
        public String rentalId;
        public String phoneNumber;
        public String rentalType;
        public String service;
        public String duration;
        public Double price;
        public String startedAt;
        public String expiresAt;
        public Boolean autoRenew;
        public String status;
        public Double retailCost;
        public String currency;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RentalActionResult {
        public boolean success;
        public String rentalId;
        public String status;
        public Double refund;
        public String newExpiresAt;
        public Double price;
        public String hoursUsed;
        public String message;
    }
}
