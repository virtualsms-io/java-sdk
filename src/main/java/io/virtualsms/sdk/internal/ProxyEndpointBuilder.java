package io.virtualsms.sdk.internal;

import io.virtualsms.sdk.model.Proxies;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, no-network composition of a ready-to-use proxy connection string.
 * Ported byte-identical to the frontend's ProxyEndpointGenerator.tsx
 * buildUsername()/buildEndpoint() logic (also mirrored in client.ts lines
 * 438-529) — this is a shared client-side contract, not a backend call, so
 * drift here silently breaks connection strings for every SDK consumer.
 */
public final class ProxyEndpointBuilder {

    /** Fixed gateway ports; rotating vs sticky is encoded in the username, not by port selection. */
    public static final int PROXY_HTTP_PORT = 823;
    public static final int PROXY_SOCKS5_PORT = 824;

    private ProxyEndpointBuilder() {
    }

    public static class Params {
        public String countryCode;
        public String targetBy = "country"; // country | state | city | zip | asn
        public String locationCode;
        public String session = "rotating"; // rotating | sticky
        public int stickyTtlMinutes = 10;
        public int count = 1;
        public String protocol = "HTTP"; // HTTP | SOCKS5
        public String format = "host:port:user:pass"; // host:port:user:pass | user:pass@host:port | curl
    }

    public static Proxies.ProxyEndpointResult build(Proxies.ProxyListItem proxy, Params params) {
        String targetBy = params.targetBy != null ? params.targetBy : "country";
        String session = params.session != null ? params.session : "rotating";
        String protocol = params.protocol != null ? params.protocol : "HTTP";
        String format = params.format != null ? params.format : "host:port:user:pass";
        int ttl = params.stickyTtlMinutes > 0 ? params.stickyTtlMinutes : 10;
        int count = Math.max(1, Math.min(100, params.count <= 0 ? 1 : params.count));
        int port = "SOCKS5".equals(protocol) ? PROXY_SOCKS5_PORT : PROXY_HTTP_PORT;

        boolean premium2x = !"country".equals(targetBy)
                && params.locationCode != null && !params.locationCode.trim().isEmpty()
                && !"residential_premium".equals(proxy.poolType);

        List<String> endpoints = new ArrayList<>();
        if ("rotating".equals(session)) {
            String user = buildUsername(proxy.proxyLogin, params.countryCode, targetBy, params.locationCode, null, null);
            String ep = buildEndpointString(proxy.proxyHost, port, user, proxy.proxyPassword, format, protocol);
            for (int i = 0; i < count; i++) endpoints.add(ep);
        } else {
            for (int i = 0; i < count; i++) {
                String user = buildUsername(proxy.proxyLogin, params.countryCode, targetBy, params.locationCode, i + 1, ttl);
                endpoints.add(buildEndpointString(proxy.proxyHost, port, user, proxy.proxyPassword, format, protocol));
            }
        }

        Proxies.ProxyEndpointResult result = new Proxies.ProxyEndpointResult();
        result.proxyId = proxy.proxyId;
        result.poolType = proxy.poolType;
        result.host = proxy.proxyHost;
        result.port = port;
        result.protocol = protocol;
        result.session = session;
        result.stickyTtlMinutes = "sticky".equals(session) ? ttl : null;
        result.countryCode = params.countryCode;
        result.targetBy = targetBy;
        result.locationCode = params.locationCode;
        result.premium2x = premium2x;
        result.endpoints = endpoints;
        return result;
    }

    private static String buildUsername(
            String login,
            String countryCode,
            String targetBy,
            String locationCode,
            Integer stickyIndex,
            Integer stickyMinutes
    ) {
        StringBuilder u = new StringBuilder(login).append("__cr.").append(countryCode.toLowerCase());
        String loc = locationCode == null ? "" : locationCode.trim();
        if (!loc.isEmpty() && !"country".equals(targetBy)) {
            switch (targetBy) {
                case "state" -> u.append(";state.").append(loc.toLowerCase());
                case "city" -> u.append(";city.").append(loc.toLowerCase());
                case "zip" -> u.append(";zip.").append(loc);
                case "asn" -> u.append(";asn.").append(loc);
                default -> { /* "country" needs no suffix */ }
            }
        }
        if (stickyIndex != null) {
            u.append(";sessid.s").append(stickyIndex).append(";sessttl.").append(stickyMinutes != null ? stickyMinutes : 10);
        }
        return u.toString();
    }

    private static String buildEndpointString(String host, int port, String user, String pass, String format, String protocol) {
        if ("host:port:user:pass".equals(format)) return host + ":" + port + ":" + user + ":" + pass;
        if ("user:pass@host:port".equals(format)) return user + ":" + pass + "@" + host + ":" + port;
        String scheme = "SOCKS5".equals(protocol) ? "socks5h" : "http";
        return "curl -x \"" + scheme + "://" + user + ":" + pass + "@" + host + ":" + port + "\" https://api.ipify.org";
    }
}
