package io.virtualsms.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualsms.sdk.exceptions.BadApiKeyException;
import io.virtualsms.sdk.exceptions.NotFoundException;
import io.virtualsms.sdk.exceptions.VirtualSmsException;
import io.virtualsms.sdk.internal.HttpExecutor;
import io.virtualsms.sdk.internal.Json;
import io.virtualsms.sdk.internal.PlatformTierCountryIds;
import io.virtualsms.sdk.internal.ProxyEndpointBuilder;
import io.virtualsms.sdk.model.Common;
import io.virtualsms.sdk.model.Orders;
import io.virtualsms.sdk.model.Proxies;
import io.virtualsms.sdk.model.Rentals;
import io.virtualsms.sdk.model.Sessions;
import io.virtualsms.sdk.model.Webhooks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Native Java client for the VirtualSMS REST API v1 (https://virtualsms.io).
 * <p>
 * This is a native REST v1 client — <b>not</b> a drop-in replacement for the
 * legacy sms-activate-compatible client library that v1.x SDKs wrapped. v2
 * talks to {@code /api/v1/*} endpoints directly.
 * <p>
 * Quickstart:
 * <pre>{@code
 * VirtualSmsClient client = new VirtualSmsClient("YOUR_API_KEY");
 * Orders.Order order = client.createOrder("tg", "GB");
 * Orders.WaitForSmsResult result = client.waitForSms(order.orderId, 300);
 * if (result.success) {
 *     System.out.println("Code: " + result.code);
 * }
 * }</pre>
 * Get an API key at https://virtualsms.io/dashboard.
 */
public final class VirtualSmsClient {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\b(\\d{4,8})\\b");
    private static final java.util.Set<String> ACTIVE_STATUSES =
            java.util.Set.of("waiting", "pending", "sms_received", "created");

    private final HttpExecutor http;
    private final String apiKey;
    private final String baseUrl;

    public VirtualSmsClient(String apiKey) {
        this(apiKey, ClientOptions.defaults());
    }

    public VirtualSmsClient(String apiKey, ClientOptions options) {
        this.apiKey = apiKey;
        this.baseUrl = options.getBaseUrl();
        this.http = new HttpExecutor(this.baseUrl, apiKey, options.getTimeoutSeconds());
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadApiKeyException(
                    "An API key is required for this operation. Get your API key at https://virtualsms.io");
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // ─── 2.1 Activations / Orders ──────────────────────────────────────────

    public List<Common.Service> listServices() {
        JsonNode res = http.get("/customer/services", null);
        JsonNode raw = res != null && res.has("services") ? res.get("services") : res;
        List<Common.Service> out = new ArrayList<>();
        if (raw != null && raw.isArray()) {
            for (JsonNode s : raw) {
                Common.Service svc = new Common.Service();
                svc.code = textOr(s, "service_id", textOr(s, "code", ""));
                svc.name = textOr(s, "service_name", textOr(s, "name", ""));
                svc.icon = s.hasNonNull("icon") ? s.get("icon").asText() : null;
                out.add(svc);
            }
        }
        return out;
    }

    public List<Common.Country> listCountries() {
        JsonNode res = http.get("/customer/countries", null);
        JsonNode raw = res != null && res.has("countries") ? res.get("countries") : res;
        List<Common.Country> out = new ArrayList<>();
        if (raw != null && raw.isArray()) {
            for (JsonNode c : raw) {
                Common.Country country = new Common.Country();
                country.iso = textOr(c, "country_id", textOr(c, "iso", ""));
                country.name = textOr(c, "country_name", textOr(c, "name", ""));
                country.flag = c.hasNonNull("flag") ? c.get("flag").asText() : null;
                out.add(country);
            }
        }
        return out;
    }

    /**
     * Check price + real stock for a service+country combo. {@code /price}
     * itself carries no availability field, so real stock is cross-checked
     * against {@code /catalog/countries}'s per-country {@code count} (count
     * &gt; 0 = in stock) — the same fail-closed two-call pattern the backend's
     * own tools use. Never trust {@code available} off {@code /price} alone.
     */
    public Common.Price getPrice(String service, String country) {
        JsonNode raw;
        try {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("service", service);
            q.put("country", country);
            raw = http.get("/price", q);
        } catch (NotFoundException e) {
            Common.Price price = new Common.Price();
            price.available = false;
            return price;
        }
        Common.Price price = new Common.Price();
        price.priceUsd = raw != null && raw.hasNonNull("price") ? raw.get("price").asDouble()
                : (raw != null && raw.hasNonNull("price_usd") ? raw.get("price_usd").asDouble() : 0.0);
        price.currency = raw != null && raw.hasNonNull("currency") ? raw.get("currency").asText() : "USD";
        price.available = false;

        try {
            List<Common.CatalogCountry> catalog = getCatalogCountries(service);
            for (Common.CatalogCountry c : catalog) {
                if (c.iso != null && c.iso.equalsIgnoreCase(country)) {
                    price.available = c.count > 0;
                    break;
                }
            }
        } catch (RuntimeException ignored) {
            // Keep the fail-closed default (false) on catalog lookup error.
        }
        return price;
    }

    public List<Common.CatalogCountry> getCatalogCountries(String service) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("service", service);
        JsonNode res = http.get("/catalog/countries", q);
        JsonNode raw = res != null && res.has("countries") ? res.get("countries")
                : (res != null && res.isArray() ? res : null);
        List<Common.CatalogCountry> out = new ArrayList<>();
        if (raw != null && raw.isArray()) {
            for (JsonNode c : raw) {
                Common.CatalogCountry cc = new Common.CatalogCountry();
                cc.iso = textOr(c, "id", textOr(c, "iso", textOr(c, "country", "")));
                cc.name = textOr(c, "name", textOr(c, "country_name", ""));
                cc.priceUsd = c.hasNonNull("price") ? c.get("price").asDouble()
                        : (c.hasNonNull("our_price") ? c.get("our_price").asDouble()
                        : (c.hasNonNull("price_usd") ? c.get("price_usd").asDouble() : 0.0));
                cc.count = c.hasNonNull("count") ? c.get("count").asInt() : 0;
                out.add(cc);
            }
        }
        return out;
    }

    public Orders.Order createOrder(String service, String country) {
        requireApiKey();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", service);
        body.put("country", country);
        return convert(http.post("/customer/purchase", body), Orders.Order.class);
    }

    public Orders.Order getOrder(String orderId) {
        requireApiKey();
        return convert(http.get("/customer/order/" + urlSafe(orderId), null), Orders.Order.class);
    }

    /** Thin wrapper over {@link #getOrder}: normalizes messages/legacy sms_code/sms_text into one shape. */
    public Orders.GetSmsResult getSms(String orderId) {
        Orders.Order order = getOrder(orderId);
        List<Common.SmsMessage> messages = normalizeMessages(order);
        String firstContent = messages.isEmpty() ? null : messages.get(0).content;
        String code = order.smsCode != null ? order.smsCode : (firstContent != null ? extractCode(firstContent) : null);

        Orders.GetSmsResult result = new Orders.GetSmsResult();
        result.status = order.status;
        result.phoneNumber = order.phoneNumber;
        if (!messages.isEmpty()) result.messages = messages;
        if (code != null) {
            result.code = code;
            result.smsCode = code;
        }
        if (firstContent != null) result.smsText = firstContent;
        return result;
    }

    /**
     * Block until an SMS arrives on the order, or timeout elapses. Default
     * timeout is 300s (5 minutes) at the SDK level — generous by design since
     * a human/script usually blocks on this, unlike the MCP tool's 60s
     * default meant for an LLM agent loop. Polling-only baseline for v2.0.0
     * (the optional WebSocket race is a v2.1 candidate — see SDK spec §1).
     * Never throws on timeout: returns a result with {@code success=false}.
     */
    public Orders.WaitForSmsResult waitForSms(String orderId) {
        return waitForSms(orderId, 300);
    }

    public Orders.WaitForSmsResult waitForSms(String orderId, int timeoutSeconds) {
        long startMs = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;
        long pollIntervalMs = 5000L;

        Orders.Order initial;
        try {
            initial = getOrder(orderId);
        } catch (RuntimeException e) {
            throw new VirtualSmsException("Failed to load order " + orderId + ": " + e.getMessage(), e);
        }
        String phoneNumber = initial.phoneNumber;

        List<Common.SmsMessage> initialMessages = normalizeMessages(initial);
        if (!initialMessages.isEmpty()) {
            return buildWaitSuccess(orderId, phoneNumber, initialMessages, "instant", null, startMs);
        }

        int attempts = 0;
        while (System.currentTimeMillis() - startMs < timeoutMs) {
            attempts++;
            try {
                Orders.Order status = getOrder(orderId);
                List<Common.SmsMessage> messages = normalizeMessages(status);
                if (!messages.isEmpty()) {
                    return buildWaitSuccess(orderId, phoneNumber, messages, "polling", attempts, startMs);
                }
                if ("cancelled".equals(status.status) || "failed".equals(status.status)) {
                    throw new VirtualSmsException("Order " + orderId + " was " + status.status + " before SMS arrived.");
                }
            } catch (VirtualSmsException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (!msg.contains("waiting") && !msg.contains("pending")) {
                    throw e;
                }
            }

            long remaining = timeoutMs - (System.currentTimeMillis() - startMs);
            if (remaining <= 0) break;
            sleepQuietly(Math.min(pollIntervalMs, remaining));
        }

        Orders.WaitForSmsResult result = new Orders.WaitForSmsResult();
        result.success = false;
        result.error = "timeout";
        result.message = "No SMS received within " + timeoutSeconds + " seconds.";
        result.orderId = orderId;
        result.phoneNumber = phoneNumber;
        result.tip = "Call getSms with this order_id later to check, or cancelOrder to refund.";
        return result;
    }

    private Orders.WaitForSmsResult buildWaitSuccess(
            String orderId, String phoneNumber, List<Common.SmsMessage> messages,
            String deliveryMethod, Integer pollAttempts, long startMs) {
        String firstContent = messages.get(0).content != null ? messages.get(0).content : "";
        String code = extractCode(firstContent);
        Orders.WaitForSmsResult result = new Orders.WaitForSmsResult();
        result.success = true;
        result.orderId = orderId;
        result.phoneNumber = phoneNumber;
        result.status = "sms_received";
        result.messages = messages;
        result.code = code;
        result.smsCode = code;
        result.smsText = firstContent;
        result.deliveryMethod = deliveryMethod;
        result.elapsedSeconds = Math.round((System.currentTimeMillis() - startMs) / 1000.0);
        result.pollAttempts = pollAttempts;
        return result;
    }

    /**
     * Cancel + refund an order (before any SMS received). Pre-checks
     * {@code cancel_available_at} from a fresh get_order call and short
     * circuits locally with a message if the 120s post-purchase cooldown is
     * still active, saving a round-trip. Best-effort: if the pre-check
     * lookup fails, the backend enforces the cooldown anyway.
     */
    public Orders.CancelResult cancelOrder(String orderId) {
        requireApiKey();
        try {
            Orders.Order order = getOrder(orderId);
            Long waitSeconds = cooldownRemainingSeconds(order.cancelAvailableAt);
            if (waitSeconds != null) {
                throw new VirtualSmsException(
                        "Cancel cooldown active. Try again in " + waitSeconds + " seconds.");
            }
        } catch (VirtualSmsException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Cancel cooldown active")) throw e;
            // Lookup failed for another reason: let the backend handle it.
        }
        return convert(http.post("/customer/cancel/" + urlSafe(orderId), Map.of()), Orders.CancelResult.class);
    }

    /** Get a new number for the same service/country, no extra charge. Same cooldown pre-check pattern as cancel. */
    public Orders.Order swapNumber(String orderId) {
        requireApiKey();
        try {
            Orders.Order order = getOrder(orderId);
            Long waitSeconds = cooldownRemainingSeconds(order.swapAvailableAt);
            if (waitSeconds != null) {
                throw new VirtualSmsException("Swap cooldown active. Try again in " + waitSeconds + " seconds.");
            }
        } catch (VirtualSmsException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Swap cooldown active")) throw e;
        }
        return convert(http.post("/customer/swap/" + urlSafe(orderId), Map.of()), Orders.Order.class);
    }

    /** Ask the provider to resend the SMS to the SAME number (not a new number; see {@link #swapNumber}). */
    public Orders.RetryOrderResult retryOrder(String orderId) {
        requireApiKey();
        return convert(http.post("/orders/" + urlSafe(orderId) + "/retry", Map.of()), Orders.RetryOrderResult.class);
    }

    /** List orders, optional status filter. A 404 (endpoint absent on older deployments) is swallowed to an empty list. */
    public List<Orders.Order> listOrders(String status) {
        requireApiKey();
        try {
            Map<String, Object> q = status != null ? Map.of("status", status) : null;
            JsonNode res = http.get("/customer/orders", q);
            JsonNode raw = res != null && res.isArray() ? res : (res != null && res.has("orders") ? res.get("orders") : null);
            List<Orders.Order> out = new ArrayList<>();
            if (raw != null && raw.isArray()) {
                for (JsonNode o : raw) {
                    Orders.Order order = new Orders.Order();
                    order.orderId = textOr(o, "order_id", textOr(o, "id", ""));
                    order.phoneNumber = textOr(o, "phone_number", "");
                    order.service = textOr(o, "service_id", textOr(o, "service", null));
                    order.country = textOr(o, "country_id", textOr(o, "country", null));
                    order.price = o.hasNonNull("price_charged") ? o.get("price_charged").asDouble()
                            : (o.hasNonNull("price") ? o.get("price").asDouble() : null);
                    order.createdAt = textOr(o, "created_at", null);
                    order.expiresAt = textOr(o, "expires_at", null);
                    order.status = textOr(o, "status", "");
                    order.smsCode = textOr(o, "sms_code", null);
                    order.smsText = textOr(o, "sms_text", null);
                    out.add(order);
                }
            }
            return out;
        } catch (NotFoundException e) {
            return List.of();
        }
    }

    public static class OrderHistoryParams {
        public String status;
        public String service;
        public String country;
        public Integer sinceDays;
        public int limit = 20;
    }

    /** Order history with client-side filtering (service/country/since_days) on top of a server-capped list_orders. */
    public Orders.OrderHistoryResult orderHistory(OrderHistoryParams params) {
        int limit = Math.min(params.limit <= 0 ? 20 : params.limit, 50);
        List<Orders.Order> orders = listOrders(params.status);

        Long cutoffMs = params.sinceDays != null ? System.currentTimeMillis() - params.sinceDays * 86_400_000L : null;
        String serviceFilter = params.service != null ? params.service.toLowerCase() : null;
        String countryFilter = params.country != null ? params.country.toUpperCase() : null;

        List<Orders.Order> filtered = new ArrayList<>();
        for (Orders.Order o : orders) {
            if (cutoffMs != null) {
                Long ts = parseDateMs(o.createdAt);
                if (ts == null || ts < cutoffMs) continue;
            }
            if (serviceFilter != null && !serviceFilter.equals(o.service == null ? null : o.service.toLowerCase())) continue;
            if (countryFilter != null && !countryFilter.equals(o.country == null ? null : o.country.toUpperCase())) continue;
            filtered.add(o);
        }

        List<Orders.Order> capped = filtered.size() > limit ? filtered.subList(0, limit) : filtered;

        Orders.OrderHistoryResult result = new Orders.OrderHistoryResult();
        result.count = capped.size();
        result.totalMatched = filtered.size();
        Orders.OrderHistoryFilters f = new Orders.OrderHistoryFilters();
        f.status = params.status;
        f.service = params.service;
        f.country = params.country;
        f.sinceDays = params.sinceDays;
        result.filters = f;
        List<Orders.OrderHistoryEntry> entries = new ArrayList<>();
        for (Orders.Order o : capped) {
            Orders.OrderHistoryEntry e = new Orders.OrderHistoryEntry();
            e.orderId = o.orderId;
            e.phoneNumber = o.phoneNumber;
            e.service = o.service;
            e.country = o.country;
            e.price = o.price;
            e.status = o.status;
            e.createdAt = o.createdAt;
            e.smsCode = o.smsCode;
            entries.add(e);
        }
        result.orders = entries;
        return result;
    }

    /** Bulk-cancel every active order. Gathers with partial failure (never aborts on first error). */
    public Orders.CancelAllOrdersResult cancelAllOrders() {
        List<Orders.Order> orders = listOrders(null);
        List<Orders.Order> active = new ArrayList<>();
        for (Orders.Order o : orders) if (ACTIVE_STATUSES.contains(o.status)) active.add(o);

        Orders.CancelAllOrdersResult result = new Orders.CancelAllOrdersResult();
        if (active.isEmpty()) {
            result.cancelled = 0;
            result.failed = 0;
            result.message = "No active orders to cancel.";
            return result;
        }

        List<Orders.CancelledOrderEntry> succeeded = new ArrayList<>();
        List<Orders.CancelFailureEntry> failures = new ArrayList<>();
        for (Orders.Order o : active) {
            try {
                Orders.CancelResult r = cancelOrder(o.orderId);
                Orders.CancelledOrderEntry entry = new Orders.CancelledOrderEntry();
                entry.orderId = o.orderId;
                entry.refunded = r.refunded;
                succeeded.add(entry);
            } catch (RuntimeException e) {
                Orders.CancelFailureEntry entry = new Orders.CancelFailureEntry();
                entry.orderId = o.orderId;
                entry.error = e.getMessage();
                failures.add(entry);
            }
        }

        result.cancelled = succeeded.size();
        result.failed = failures.size();
        result.totalActive = active.size();
        result.cancelledOrders = succeeded;
        result.failures = failures;
        return result;
    }

    /** Find the right service code using natural language ("uber", "binance", "steam"). */
    public Orders.SearchServicesResult searchServices(String query) {
        List<Common.Service> services = listServices();
        String q = query.toLowerCase().trim();

        List<Orders.ServiceMatch> scored = new ArrayList<>();
        for (Common.Service s : services) {
            String name = s.name.toLowerCase();
            String code = s.code.toLowerCase();
            double score;
            if (code.equals(q) || name.equals(q)) {
                score = 1.0;
            } else if (code.startsWith(q) || name.startsWith(q)) {
                score = 0.9;
            } else if (code.contains(q) || name.contains(q)) {
                score = 0.7;
            } else {
                String[] queryTokens = q.split("\\s+");
                String[] nameTokens = name.split("[\\s_-]+");
                int matches = 0;
                for (String qt : queryTokens) {
                    for (String nt : nameTokens) {
                        if (nt.contains(qt) || qt.contains(nt)) {
                            matches++;
                            break;
                        }
                    }
                }
                score = matches > 0 ? (matches / (double) Math.max(queryTokens.length, nameTokens.length)) * 0.6 : 0;
            }
            if (score > 0) {
                Orders.ServiceMatch m = new Orders.ServiceMatch();
                m.code = s.code;
                m.name = s.name;
                m.matchScore = Math.round(score * 100) / 100.0;
                scored.add(m);
            }
        }

        List<Orders.ServiceMatch> matches = new ArrayList<>();
        scored.sort((a, b) -> Double.compare(b.matchScore, a.matchScore));
        for (Orders.ServiceMatch m : scored) {
            if (m.matchScore >= 0.5) matches.add(m);
            if (matches.size() == 5) break;
        }

        Orders.SearchServicesResult result = new Orders.SearchServicesResult();
        result.query = query;
        result.matches = matches;
        if (matches.isEmpty()) {
            result.message = "No matching services found";
            result.tip = "Try listServices to browse all available services.";
        } else {
            result.tip = "Use the \"code\" field as the service parameter in other methods.";
        }
        return result;
    }

    /** Find the cheapest in-stock countries for a service, sorted by price. Default limit 5. */
    public Orders.FindCheapestResult findCheapest(String service, Integer limit) {
        int lim = limit != null && limit > 0 ? limit : 5;
        List<Common.CatalogCountry> catalog = getCatalogCountries(service);

        List<Orders.CheapestOption> results = new ArrayList<>();
        for (Common.CatalogCountry c : catalog) {
            if (c.count > 0) {
                Orders.CheapestOption opt = new Orders.CheapestOption();
                opt.country = c.iso;
                opt.countryName = c.name;
                opt.priceUsd = c.priceUsd;
                opt.stock = true;
                results.add(opt);
            }
        }
        results.sort((a, b) -> Double.compare(a.priceUsd, b.priceUsd));

        Orders.FindCheapestResult result = new Orders.FindCheapestResult();
        result.service = service;
        result.totalAvailableCountries = results.size();
        if (results.isEmpty()) {
            result.cheapestOptions = List.of();
            result.message = "No countries available for service \"" + service + "\". Use searchServices to verify " +
                    "the service code, or listServices to see all available services.";
        } else {
            result.cheapestOptions = results.size() > lim ? results.subList(0, lim) : results;
        }
        return result;
    }

    // ─── 2.2 Rentals ────────────────────────────────────────────────────────

    public List<Rentals.RentalPricingTier> rentalsPricing() {
        JsonNode res = http.get("/rentals/pricing", null);
        return convertList(res, Rentals.RentalPricingTier.class);
    }

    public static class RentalsAvailableParams {
        public String country;
        public String service;
        public String type; // "service" | "full"
        public String tier; // "full_access" | "platform"
    }

    public Rentals.RentalAvailabilityResult rentalsAvailable(RentalsAvailableParams params) {
        Map<String, Object> q = new LinkedHashMap<>();
        if (params != null) {
            q.put("country", params.country);
            q.put("service", params.service);
            q.put("type", params.type);
            q.put("provider", "platform".equals(params.tier) ? "network" : null);
        }
        return convert(http.get("/rentals/available", q), Rentals.RentalAvailabilityResult.class);
    }

    /** Platform-tier services available in a country w/ stock + retail price. Explicit field allowlist via Jackson binding. */
    public List<Rentals.RentalCatalogService> rentalsServices(String countryCode, Integer durationHours) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("country_code", countryCode);
        q.put("duration", durationHours != null ? durationHours : 24);
        JsonNode res = http.get("/rentals/services", q);
        return convertList(res, Rentals.RentalCatalogService.class);
    }

    public Rentals.RentalPriceResult rentalsPrice(String service, String countryCode, int durationHours) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("service", service);
        q.put("country_code", countryCode);
        q.put("duration", durationHours);
        return convert(http.get("/rentals/price", q), Rentals.RentalPriceResult.class);
    }

    public static class CreateRentalParams {
        public String tier; // "full_access" | "platform", required
        public String country; // ISO-2 country code, required
        public int durationHours;
        public String service; // required for platform tier
        public boolean autoRenew; // full_access only
    }

    /**
     * Create a rental. {@code full_access}: local SIM inventory, any service.
     * {@code platform}: sourced via our global supplier network, one service
     * per number; resolves the ISO country code to the internal numeric ID
     * via {@link PlatformTierCountryIds} — callers never pass the numeric ID.
     */
    public Rentals.CreateRentalResult createRental(CreateRentalParams params) {
        requireApiKey();
        if ("platform".equals(params.tier)) {
            Integer countryId = PlatformTierCountryIds.MAP.get(params.country.toUpperCase());
            if (countryId == null) {
                throw new VirtualSmsException(
                        "Platform-tier rentals are not available for country_code \"" + params.country + "\". " +
                        "Use rentalsAvailable with tier=platform to see supported countries.");
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("service", params.service);
            body.put("country", countryId);
            body.put("duration_hours", params.durationHours);
            body.put("provider", "network");
            JsonNode data = http.post("/rentals/provider", body);
            Rentals.CreateRentalResult result = new Rentals.CreateRentalResult();
            result.success = data == null || !data.hasNonNull("success") || data.get("success").asBoolean(true);
            result.rentalId = textOr(data, "rental_id", "");
            result.phoneNumber = textOr(data, "phone_number", "");
            result.expiresAt = textOr(data, "expires_at", "");
            result.retailCost = data != null && data.hasNonNull("retail_cost") ? data.get("retail_cost").asDouble() : null;
            result.currency = textOr(data, "currency", null);
            result.status = "active";
            return result;
        }
        // rental_type is inferred: a chosen service means a single-service
        // rental ("service"), no service means a whole-line rental ("full").
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("country", params.country);
        body.put("rental_type", params.service != null ? "service" : "full");
        body.put("duration_hours", params.durationHours);
        body.put("service", params.service);
        body.put("auto_renew", params.autoRenew);
        return convert(http.post("/rentals", body), Rentals.CreateRentalResult.class);
    }

    public List<Rentals.Rental> listRentals(String status) {
        requireApiKey();
        Map<String, Object> q = status != null ? Map.of("status", status) : null;
        JsonNode res = http.get("/rentals", q);
        return convertList(res, Rentals.Rental.class);
    }

    /** No dedicated GET-by-id backend route exists; this is a client-side {@code listRentals("all")} lookup by design. */
    public Rentals.Rental getRental(String rentalId) {
        for (Rentals.Rental r : listRentals("all")) {
            if (rentalId.equals(r.id)) return r;
        }
        return null;
    }

    public Rentals.RentalActionResult extendRental(String rentalId, int durationHours) {
        requireApiKey();
        Map<String, Object> body = Map.of("duration_hours", durationHours);
        return convert(http.post("/rentals/" + urlSafe(rentalId) + "/extend", body), Rentals.RentalActionResult.class);
    }

    /** Full refund: only within 20 minutes of purchase and before the first SMS. Either tier. */
    public Rentals.RentalActionResult cancelRental(String rentalId) {
        requireApiKey();
        return convert(http.post("/rentals/" + urlSafe(rentalId) + "/cancel", Map.of()), Rentals.RentalActionResult.class);
    }

    // ─── 2.3 Proxies ────────────────────────────────────────────────────────

    public List<Proxies.ProxyCatalogPoolType> listProxyCatalog() {
        JsonNode res = http.get("/proxies/catalog", null);
        JsonNode raw = res != null && res.has("pool_types") ? res.get("pool_types") : res;
        return convertList(raw, Proxies.ProxyCatalogPoolType.class);
    }

    public List<Proxies.ProxyListItem> listProxies() {
        requireApiKey();
        JsonNode res = http.get("/proxies", null);
        return convertList(res, Proxies.ProxyListItem.class);
    }

    public static class BuyProxyParams {
        public String poolType; // residential | residential_premium | mobile | datacenter, required
        public double gb; // required, positive
        public String countryCode; // soft preference only
        public String idempotencyKey;
    }

    public Proxies.ProxyPurchaseResult buyProxy(BuyProxyParams params) {
        requireApiKey();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pool_type", params.poolType);
        body.put("gb", params.gb);
        body.put("country_code", params.countryCode);
        body.put("idempotency_key", params.idempotencyKey);
        return convert(http.post("/proxies", body), Proxies.ProxyPurchaseResult.class);
    }

    public Proxies.ProxyRotateResult rotateProxy(String proxyId, Integer port) {
        requireApiKey();
        Map<String, Object> body = port != null ? Map.of("port", port) : Map.of();
        return convert(http.post("/proxies/" + urlSafe(proxyId) + "/rotate", body), Proxies.ProxyRotateResult.class);
    }

    /** Cached GB used/remaining (refreshed ~5min, no upstream call). */
    public Proxies.ProxyUsage getProxyUsage(String proxyId) {
        requireApiKey();
        return convert(http.get("/proxies/" + urlSafe(proxyId) + "/usage", null), Proxies.ProxyUsage.class);
    }

    /** Per-day GB/requests series, 7d or 30d (default 7d). */
    public Proxies.ProxyUsageHistoryResult getProxyUsageHistory(String proxyId, String range) {
        requireApiKey();
        Map<String, Object> q = range != null ? Map.of("range", range) : null;
        return convert(http.get("/proxies/" + urlSafe(proxyId) + "/usage-history", q), Proxies.ProxyUsageHistoryResult.class);
    }

    public static class SetProxyTargetingParams {
        public String countryCode; // required
        public List<String> cities;
        public List<Integer> asns;
    }

    /**
     * Persist default geo-targeting on a proxy sub-user. Country-only is
     * free; cities/asns bill 2x GB on non-premium pools (free on
     * residential_premium — the response's {@code premium2x} flags this).
     */
    public Proxies.ProxyTargetingResult setProxyTargeting(String proxyId, SetProxyTargetingParams params) {
        requireApiKey();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("country_code", params.countryCode);
        body.put("cities", params.cities);
        body.put("asns", params.asns);
        return convert(http.post("/proxies/" + urlSafe(proxyId) + "/targeting", body), Proxies.ProxyTargetingResult.class);
    }

    public static class TestProxyParams {
        public String country; // required
        public String session; // "rotating" | "sticky"
        public String protocol; // "http" | "socks5"
    }

    /** Dial out through the proxy, report exit IP/country/city/ISP/latency. Rate-limited ~1/20s per proxy. */
    public Proxies.ProxyTestResult testProxy(String proxyId, TestProxyParams params) {
        requireApiKey();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("country", params.country);
        body.put("session", params.session);
        body.put("protocol", params.protocol);
        return convert(http.post("/proxies/" + urlSafe(proxyId) + "/test", body), Proxies.ProxyTestResult.class);
    }

    /** Discover valid cities/states/asns/zips for a pool_type+country. Public, no auth. Not available for residential_premium. */
    public List<Proxies.ProxyLocationItem> listProxyLocations(String poolType, String country, String kind) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("pool_type", poolType);
        q.put("country", country);
        q.put("kind", kind);
        JsonNode res = http.get("/proxies/locations", q);
        JsonNode raw = res != null && res.has("items") ? res.get("items") : res;
        return convertList(raw, Proxies.ProxyLocationItem.class);
    }

    /**
     * Compose a ready-to-use proxy connection string. No backend call, no
     * purchase — pure client-side composition ported byte-identical to the
     * frontend's ProxyEndpointGenerator (see {@link ProxyEndpointBuilder}).
     * Looks up the proxy's credentials via {@link #listProxies()} first.
     */
    public Proxies.ProxyEndpointResult generateProxyEndpoint(String proxyId, ProxyEndpointBuilder.Params params) {
        requireApiKey();
        List<Proxies.ProxyListItem> proxies = listProxies();
        Proxies.ProxyListItem proxy = null;
        for (Proxies.ProxyListItem p : proxies) {
            if (proxyId.equals(p.proxyId)) {
                proxy = p;
                break;
            }
        }
        if (proxy == null) {
            throw new NotFoundException("Not found: proxy " + proxyId + " does not exist on this account");
        }
        return ProxyEndpointBuilder.build(proxy, params);
    }

    // ─── 2.4 Account ────────────────────────────────────────────────────────

    public Common.Balance getBalance() {
        requireApiKey();
        return convert(http.get("/customer/balance", null), Common.Balance.class);
    }

    public Common.Profile getProfile() {
        requireApiKey();
        return convert(http.get("/customer/profile", null), Common.Profile.class);
    }

    public static class GetTransactionsParams {
        public String type; // deposit | purchase | refund | admin_credit
        public String from; // RFC3339 or YYYY-MM-DD
        public String to;
        public Integer limit; // 1-200, default 50
        public Integer offset;
    }

    public Common.TransactionsPage getTransactions(GetTransactionsParams params) {
        requireApiKey();
        Map<String, Object> q = new LinkedHashMap<>();
        if (params != null) {
            q.put("type", params.type);
            q.put("from", params.from);
            q.put("to", params.to);
            q.put("limit", params.limit);
            q.put("offset", params.offset);
        }
        return convert(http.get("/customer/transactions", q), Common.TransactionsPage.class);
    }

    /** Aggregated usage stats over a lookback window (default 30 days), assembled client-side from get_balance + list_orders. */
    public Orders.StatsResult getStats(Integer sinceDays) {
        int days = sinceDays != null ? sinceDays : 30;
        long cutoffMs = System.currentTimeMillis() - days * 86_400_000L;

        Common.Balance balance = getBalance();
        List<Orders.Order> orders = listOrders(null);

        List<Orders.Order> inWindow = new ArrayList<>();
        for (Orders.Order o : orders) {
            Long ts = parseDateMs(o.createdAt);
            if (ts != null && ts >= cutoffMs) inWindow.add(o);
        }

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        Map<String, Integer> byService = new LinkedHashMap<>();
        Map<String, Integer> byCountry = new LinkedHashMap<>();
        double totalSpend = 0;
        int successful = 0;
        int terminal = 0;
        java.util.Set<String> terminalStatuses = java.util.Set.of("completed", "sms_received", "expired", "cancelled");

        for (Orders.Order o : inWindow) {
            byStatus.merge(o.status, 1, Integer::sum);
            if (o.service != null) byService.merge(o.service, 1, Integer::sum);
            if (o.country != null) byCountry.merge(o.country, 1, Integer::sum);
            if (!"cancelled".equals(o.status) && o.price != null) totalSpend += o.price;
            if (terminalStatuses.contains(o.status)) {
                terminal++;
                if ("completed".equals(o.status) || "sms_received".equals(o.status)) successful++;
            }
        }

        Orders.StatsResult result = new Orders.StatsResult();
        result.windowDays = days;
        result.balanceUsd = balance.balanceUsd;
        result.totalOrders = inWindow.size();
        result.successfulOrders = successful;
        result.successRate = terminal > 0 ? Math.round((successful / (double) terminal) * 1000) / 10.0 : null;
        result.totalSpendUsd = Math.round(totalSpend * 100) / 100.0;
        result.statusBreakdown = byStatus;
        result.topServices = topEntries(byService);
        result.topCountries = topEntries(byCountry);
        if (orders.size() >= 50) {
            result.note = "Server caps order history at 50 rows. Stats may undercount if your activity exceeds 50 orders in the window.";
        }
        return result;
    }

    private List<Orders.KeyCount> topEntries(Map<String, Integer> counts) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());
        List<Orders.KeyCount> out = new ArrayList<>();
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            Orders.KeyCount kc = new Orders.KeyCount();
            kc.key = entries.get(i).getKey();
            kc.count = entries.get(i).getValue();
            out.add(kc);
        }
        return out;
    }

    // ─── 2.5 Session ────────────────────────────────────────────────────────

    public static class StartSessionParams {
        public String serviceName;
        public String country;
        public String deviceMode; // desktop | mobile
        public Boolean withProxy;
        public String targetUrl;
        public String orderId;
        public String mode; // attach | fresh, default fresh
    }

    /**
     * Start a country-matched cloud browser session the caller drives
     * manually via {@code viewerUrl}. Beta, invite-only. On a beta-gate
     * signal (403/404/503) this throws a {@link VirtualSmsException} with a
     * clean "invite-only beta" message rather than a raw HTTP error.
     */
    public Sessions.BrowserSessionResult startManualRegistrationSession(StartSessionParams params) {
        requireApiKey();
        boolean withProxy = params.withProxy != null ? params.withProxy : params.country != null;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("serviceName", params.serviceName);
        body.put("country", params.country);
        body.put("deviceMode", params.deviceMode);
        body.put("withProxy", withProxy);
        body.put("targetUrl", params.targetUrl);
        body.put("orderId", params.orderId);
        body.put("mode", params.mode != null ? params.mode : "fresh");
        try {
            JsonNode res = http.post("/browser-sessions/start", body);
            JsonNode session = res != null && res.has("session") ? res.get("session") : res;
            return convert(session, Sessions.BrowserSessionResult.class);
        } catch (VirtualSmsException e) {
            Integer status = e.getStatusCode();
            if (status != null && (status == 403 || status == 404 || status == 503)) {
                throw new VirtualSmsException(
                        "Manual registration sessions are an invite-only beta. Join https://t.me/VirtualSMS_io to request access.");
            }
            throw e;
        }
    }

    // ─── 2.6 Other ──────────────────────────────────────────────────────────

    /** Carrier + line-type lookup for an arbitrary E.164 number. Public, no auth. */
    public Common.NumberCheckResult checkNumber(String number) {
        Map<String, Object> q = Map.of("number", number);
        return convert(http.get("/tools/number-check", q), Common.NumberCheckResult.class);
    }

    // ─── 2.7 Webhooks ───────────────────────────────────────────────────────

    public Webhooks.ListWebhooksResult listWebhooks() {
        requireApiKey();
        return convert(http.get("/customer/webhooks", null), Webhooks.ListWebhooksResult.class);
    }

    public static class CreateWebhookParams {
        public String url; // required, https://, no localhost/IP literals
        public String description;
        public List<String> events; // required, non-empty
        public Double threshold; // required if events includes "balance.low"; 0 < n <= 99999.99
    }

    /** Secret is returned exactly once, on create only — store it immediately. */
    public Webhooks.WebhookEndpoint createWebhook(CreateWebhookParams params) {
        requireApiKey();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", params.url);
        body.put("description", params.description);
        body.put("events", params.events);
        body.put("threshold", params.threshold);
        JsonNode res = http.post("/customer/webhooks", body);
        JsonNode webhook = res != null && res.has("webhook") ? res.get("webhook") : res;
        return convert(webhook, Webhooks.WebhookEndpoint.class);
    }

    public Webhooks.WebhookEndpoint getWebhook(String id) {
        requireApiKey();
        JsonNode res = http.get("/customer/webhooks/" + urlSafe(id), null);
        JsonNode webhook = res != null && res.has("webhook") ? res.get("webhook") : res;
        return convert(webhook, Webhooks.WebhookEndpoint.class);
    }

    public static class UpdateWebhookParams {
        public String url;
        public String description;
        public List<String> events;
        public Double threshold;
        public Boolean active;
        public Boolean paused; // un-pausing (false when previously true) resets failure_count_consecutive server-side
    }

    /** Partial update. At least one field is required (the backend 400s on an empty updatable body). */
    public Webhooks.WebhookEndpoint updateWebhook(String id, UpdateWebhookParams params) {
        requireApiKey();
        Map<String, Object> body = new LinkedHashMap<>();
        if (params.url != null) body.put("url", params.url);
        if (params.description != null) body.put("description", params.description);
        if (params.events != null) body.put("events", params.events);
        if (params.threshold != null) body.put("threshold", params.threshold);
        if (params.active != null) body.put("active", params.active);
        if (params.paused != null) body.put("paused", params.paused);
        JsonNode res = http.patch("/customer/webhooks/" + urlSafe(id), body);
        JsonNode webhook = res != null && res.has("webhook") ? res.get("webhook") : res;
        return convert(webhook, Webhooks.WebhookEndpoint.class);
    }

    public Webhooks.DeleteWebhookResult deleteWebhook(String id) {
        requireApiKey();
        return convert(http.delete("/customer/webhooks/" + urlSafe(id)), Webhooks.DeleteWebhookResult.class);
    }

    /** Fire a synthetic test event through the real dispatcher. Requires the webhook to be active and not paused. */
    public Webhooks.TestWebhookResult testWebhook(String id) {
        requireApiKey();
        return convert(http.post("/customer/webhooks/" + urlSafe(id) + "/test", Map.of()), Webhooks.TestWebhookResult.class);
    }

    public Webhooks.ListWebhookDeliveriesResult listWebhookDeliveries(String id, Integer limit, Integer offset) {
        requireApiKey();
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("limit", limit != null ? Math.min(limit, 500) : 100);
        q.put("offset", offset);
        return convert(http.get("/customer/webhooks/" + urlSafe(id) + "/deliveries", q), Webhooks.ListWebhookDeliveriesResult.class);
    }

    // ─── internal helpers ───────────────────────────────────────────────────

    private static String textOr(JsonNode node, String field, String fallback) {
        if (node == null || !node.hasNonNull(field)) return fallback;
        return node.get(field).asText();
    }

    private static String urlSafe(String raw) {
        return java.net.URLEncoder.encode(raw, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String extractCode(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher m = CODE_PATTERN.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static List<Common.SmsMessage> normalizeMessages(Orders.Order order) {
        if (order.messages != null && !order.messages.isEmpty()) return order.messages;
        if ((order.smsText != null && !order.smsText.isEmpty()) || (order.smsCode != null && !order.smsCode.isEmpty())) {
            Common.SmsMessage m = new Common.SmsMessage();
            m.content = order.smsText != null ? order.smsText : order.smsCode;
            return List.of(m);
        }
        return List.of();
    }

    /** Returns remaining cooldown seconds if {@code availableAt} is still in the future, else null. */
    private static Long cooldownRemainingSeconds(String availableAt) {
        if (availableAt == null || availableAt.isBlank()) return null;
        try {
            long availableMs = java.time.Instant.parse(availableAt).toEpochMilli();
            long now = System.currentTimeMillis();
            if (now >= availableMs) return null;
            return (long) Math.ceil((availableMs - now) / 1000.0);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Long parseDateMs(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return java.time.Instant.parse(value).toEpochMilli();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static <T> T convert(JsonNode node, Class<T> type) {
        if (node == null) {
            throw new VirtualSmsException("Empty response body from VirtualSMS API");
        }
        try {
            return Json.MAPPER.treeToValue(node, type);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new VirtualSmsException("Failed to parse VirtualSMS API response: " + e.getMessage(), e);
        }
    }

    private static <T> List<T> convertList(JsonNode node, Class<T> elementType) {
        if (node == null || !node.isArray()) return List.of();
        try {
            return Json.MAPPER.convertValue(node, Json.MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (RuntimeException e) {
            throw new VirtualSmsException("Failed to parse VirtualSMS API response: " + e.getMessage(), e);
        }
    }
}
