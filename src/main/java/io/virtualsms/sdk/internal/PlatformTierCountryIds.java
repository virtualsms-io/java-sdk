package io.virtualsms.sdk.internal;

import java.util.Map;

/**
 * Internal ISO-3166 alpha-2 -> platform-network numeric country ID map.
 * Required only by the platform-tier create-rental call (the backend's
 * create endpoint takes a numeric ID; every other rentals endpoint resolves
 * country_code server-side). Ported verbatim from the MCP client
 * (client.ts PLATFORM_TIER_COUNTRY_IDS, lines 411-429) — same table already
 * shipped in the customer-facing frontend bundle for the same purpose. Not
 * every ISO code the platform lists is rental-capable; an unmapped code
 * means that country isn't available for platform-tier rentals.
 */
public final class PlatformTierCountryIds {

    private PlatformTierCountryIds() {
    }

    public static final Map<String, Integer> MAP = Map.ofEntries(
            Map.entry("RU", 0), Map.entry("UA", 1), Map.entry("KZ", 2), Map.entry("CN", 3),
            Map.entry("PH", 4), Map.entry("MM", 5), Map.entry("ID", 6), Map.entry("MY", 7),
            Map.entry("KE", 8), Map.entry("TZ", 9), Map.entry("VN", 10), Map.entry("KG", 11),
            Map.entry("IL", 13), Map.entry("HK", 14), Map.entry("PL", 15), Map.entry("GB", 16),
            Map.entry("MG", 17), Map.entry("CD", 18), Map.entry("NG", 19), Map.entry("MO", 20),
            Map.entry("EG", 21), Map.entry("IN", 22), Map.entry("IE", 23), Map.entry("KH", 24),
            Map.entry("LA", 25), Map.entry("HT", 26), Map.entry("CI", 27), Map.entry("GM", 28),
            Map.entry("RS", 29), Map.entry("YE", 30), Map.entry("ZA", 31), Map.entry("RO", 32),
            Map.entry("CO", 33), Map.entry("EE", 34), Map.entry("AZ", 35), Map.entry("CA", 36),
            Map.entry("MA", 37), Map.entry("GH", 38), Map.entry("AR", 39), Map.entry("UZ", 40),
            Map.entry("CM", 41), Map.entry("TD", 42), Map.entry("DE", 43), Map.entry("LT", 44),
            Map.entry("HR", 45), Map.entry("SE", 46), Map.entry("IQ", 47), Map.entry("NL", 48),
            Map.entry("LV", 49), Map.entry("AT", 50), Map.entry("BY", 51), Map.entry("TH", 52),
            Map.entry("SA", 53), Map.entry("MX", 54), Map.entry("TW", 55), Map.entry("ES", 56),
            Map.entry("IR", 57), Map.entry("DZ", 58), Map.entry("SI", 59), Map.entry("BD", 60),
            Map.entry("SN", 61), Map.entry("TR", 62), Map.entry("CZ", 63), Map.entry("LK", 64),
            Map.entry("PE", 65), Map.entry("PK", 66), Map.entry("NZ", 67), Map.entry("GN", 68),
            Map.entry("ML", 69), Map.entry("VE", 70), Map.entry("ET", 71), Map.entry("MN", 72),
            Map.entry("BR", 73), Map.entry("AF", 74), Map.entry("UG", 75), Map.entry("AO", 76),
            Map.entry("CY", 77), Map.entry("FR", 78), Map.entry("PG", 79), Map.entry("MZ", 80),
            Map.entry("NP", 81), Map.entry("BE", 82), Map.entry("BG", 83), Map.entry("HU", 84),
            Map.entry("MD", 85), Map.entry("IT", 86), Map.entry("PY", 87), Map.entry("HN", 88),
            Map.entry("TN", 89), Map.entry("NI", 90), Map.entry("TL", 91), Map.entry("BO", 92),
            Map.entry("CR", 93), Map.entry("GT", 94), Map.entry("AE", 95), Map.entry("ZW", 96),
            Map.entry("PR", 97), Map.entry("SD", 98), Map.entry("TG", 99), Map.entry("KW", 100),
            Map.entry("SV", 101), Map.entry("LY", 102), Map.entry("JM", 103), Map.entry("TT", 104),
            Map.entry("EC", 105), Map.entry("SZ", 106), Map.entry("OM", 107), Map.entry("BA", 108),
            Map.entry("DO", 109), Map.entry("SY", 110), Map.entry("QA", 111), Map.entry("PA", 112),
            Map.entry("CU", 113), Map.entry("MR", 114), Map.entry("SL", 115), Map.entry("JO", 116),
            Map.entry("PT", 117), Map.entry("BB", 118), Map.entry("BI", 119), Map.entry("BJ", 120),
            Map.entry("BN", 121), Map.entry("BS", 122), Map.entry("BW", 123), Map.entry("CF", 125),
            Map.entry("GD", 127), Map.entry("GE", 128), Map.entry("GR", 129), Map.entry("GW", 130),
            Map.entry("GY", 131), Map.entry("IS", 132), Map.entry("KM", 133), Map.entry("KN", 134),
            Map.entry("LR", 135), Map.entry("LS", 136), Map.entry("MW", 137), Map.entry("NA", 138),
            Map.entry("NE", 139), Map.entry("RW", 140), Map.entry("SK", 141), Map.entry("SR", 142),
            Map.entry("TJ", 143), Map.entry("MC", 144), Map.entry("BH", 145), Map.entry("RE", 146),
            Map.entry("ZM", 147), Map.entry("US", 187)
    );
}
