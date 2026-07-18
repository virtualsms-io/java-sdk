# VirtualSMS Java SDK

## What is VirtualSMS?

Official Java SDK for the VirtualSMS API. VirtualSMS is an account verification platform for
individuals, developers, and AI agents: one-time SMS verification, dedicated number rentals,
matching-country proxies, and private cloud browser sessions (beta), all behind one API, one
MCP server, and one prepaid balance. This library wraps the REST API in native Java, backed by
real carrier-issued mobile numbers (real physical SIM cards, not VoIP) across 2500+ services
in 145+ countries.

Built for developers and AI agents: REST API, hosted MCP server, SDKs.

> **v2.0.0 is a native REST v1 client.** It talks to `/api/v1/*` endpoints
> directly. It is **not** a drop-in replacement for a legacy
> sms-activate-compatible client library -  that framing belonged to v1.x
> only and has been dropped.

## Install

Maven:

```xml
<dependency>
  <groupId>io.virtualsms</groupId>
  <artifactId>sdk</artifactId>
  <version>2.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'io.virtualsms:sdk:2.0.0'
```

## Quickstart

<!-- TODO: re-point to /dashboard once the frontend migration ships -->
1. Get an API key at [virtualsms.io](https://virtualsms.io).
2. Buy a number, wait for the code:

```java
import io.virtualsms.sdk.VirtualSmsClient;
import io.virtualsms.sdk.model.Orders;

VirtualSmsClient client = new VirtualSmsClient("YOUR_API_KEY");

Orders.Order order = client.createOrder("tg", "GB"); // Telegram, UK number
System.out.println("Number: " + order.phoneNumber);

Orders.WaitForSmsResult result = client.waitForSms(order.orderId); // blocks up to 300s by default
if (result.success) {
    System.out.println("Code: " + result.code);
} else {
    client.cancelOrder(order.orderId); // refund if nothing arrived
}
```

## Configuration

```java
import io.virtualsms.sdk.ClientOptions;

VirtualSmsClient client = new VirtualSmsClient(
    "YOUR_API_KEY",
    ClientOptions.builder()
        .baseUrl("https://virtualsms.io/api/v1") // default; also overridable via VIRTUALSMS_BASE_URL env var
        .timeoutSeconds(30)                       // default
        .build()
);
```

## Capabilities

1. One-time SMS verification. Receive a code for a service like WhatsApp, Telegram, Discord,
   or a dating app, on demand, from $0.05 per code.
2. Dedicated number rentals. Hold one number for 1-30 days and receive SMS from any service
   on that number, from $0.25/day.
3. Matching-country proxies. Pair a number with an IP from the same country, across 223
   proxy countries, from $1.10/GB.
4. Private cloud browser sessions (beta). Start a country-matched browser in a live viewer
   for the signup step itself, invite-only.

## Why real SIM cards

VirtualSMS runs on real carrier-issued mobile numbers, backed by real physical SIM cards,
not VoIP. Services like WhatsApp, Telegram, Discord, and dating apps run a carrier lookup
before they send a code, and VoIP or virtual numbers fail that check more often than a real
SIM does. A physical SIM on a real carrier network reads like any other phone on that network,
carriers like Vodafone, O2, and T-Mobile depending on the country, which is part of why
VirtualSMS holds a 95%+ success rate across 2500+ services in 145+ countries.

## API coverage

Method coverage (46 methods):

| Group | Count | Examples |
|---|---|---|
| Activations / Orders | 15 | `listServices`, `getPrice`, `createOrder`, `waitForSms`, `cancelOrder`, `findCheapest` |
| Rentals | 9 | `rentalsAvailable`, `createRental`, `extendRental`, `cancelRental` |
| Proxies | 10 | `listProxyCatalog`, `buyProxy`, `rotateProxy`, `generateProxyEndpoint` |
| Account | 4 | `getBalance`, `getProfile`, `getTransactions`, `getStats` |
| Session (beta) | 1 | `startManualRegistrationSession` |
| Tools | 1 | `checkNumber` |
| Webhooks | 7 | `listWebhooks`, `createWebhook`, `updateWebhook`, `testWebhook` |

See `examples/` for full activation, rental, and proxy flows.

## Error handling

Every error maps to a typed exception under `io.virtualsms.sdk.exceptions`,
all extending `VirtualSmsException`:

| Exception | Cause |
|---|---|
| `BadApiKeyException` | HTTP 401 -  invalid or missing key |
| `InsufficientBalanceException` | HTTP 402 -  top up your balance |
| `NotFoundException` | HTTP 404 |
| `RateLimitedException` | HTTP 429 -  never auto-retried |
| `ServerErrorException` | HTTP 5xx -  check `isMutatingRequest()` before retrying |
| `ApiException` | any other 4xx |

```java
try {
    client.createOrder("tg", "GB");
} catch (InsufficientBalanceException e) {
    System.out.println("Top up: " + e.getMessage());
} catch (VirtualSmsException e) {
    System.out.println("Failed: " + e.getMessage());
}
```

GET requests are retried automatically (up to 3 attempts, exponential
backoff) on network failure or a 5xx. Mutating requests (POST/PUT/PATCH/
DELETE) are **never** auto-retried -  a 5xx on a purchase/cancel/rotate call
does not prove the operation failed server-side; verify with a read call
first.

## Requirements

- Java 17+
- Jackson (`jackson-databind`) -  the SDK's only runtime dependency

## Rentals: two tiers

- **Full Access** -  local SIM inventory, usable for any service, longer durations.
- **Platform** -  sourced via our global supplier network, locked to one chosen service, short (24/72/168h) durations.

Both tiers carry the same refund terms: full refund within 20 minutes of
purchase and before the first SMS arrives.

## AI agents and MCP

This SDK is the API-client half of VirtualSMS: typed methods you call directly from your own
Java code. VirtualSMS also exposes a hosted MCP server, so an AI agent such as Claude or
Cursor can request a number, wait for a code, or manage a rental the same way this SDK does,
without you writing the glue code yourself. See [virtualsms.io/docs](https://virtualsms.io/docs)
for MCP server details.

## FAQ

### What is VirtualSMS?
VirtualSMS is an account verification platform for individuals, developers, and AI agents. It combines one-time SMS verification, dedicated number rentals, matching-country proxies, and private cloud browser sessions behind one API, one MCP server, and one prepaid balance.

### Does VirtualSMS use real SIM cards or VoIP numbers?
VirtualSMS uses real carrier-issued mobile numbers, backed by real physical SIM cards, not VoIP. Many services, including WhatsApp, Telegram, Discord, and dating apps, reject VoIP and virtual numbers at signup; a real physical SIM on a real carrier network passes that check far more often, which is reflected in a 95%+ success rate.

### Which services and countries does VirtualSMS support?
VirtualSMS covers 2500+ services across 145+ countries for SMS verification and number rentals, plus matching-country proxies across 223 proxy countries. Coverage spans messaging apps, social platforms, marketplaces, dating apps, and financial services.

### Can I rent a number, or only buy one-time codes?
Both. Buy a single one-time code from $0.05, or rent a dedicated number for 1-30 days from $0.25/day to receive SMS from any service on that number for the rental window.

### Does VirtualSMS work with AI agents and MCP?
Yes. VirtualSMS exposes a hosted MCP server plus a REST API and official SDKs in nine languages, so an AI agent can request a number, wait for a code, or manage a rental the same way a developer would call the API directly.

### How much does VirtualSMS cost?
Pricing is pay-as-you-go from one prepaid balance: SMS verification from $0.05 per code, number rentals from $0.25/day, and proxies from $1.10/GB. There is no subscription requirement.

### Is there a free API key?
Yes. Creating a VirtualSMS account issues an API key immediately, at no cost. You only spend from your prepaid balance when you place an order: an activation, a rental, or a proxy.

## Links

- Website: [virtualsms.io](https://virtualsms.io)
- Docs: [virtualsms.io/docs](https://virtualsms.io/docs)
- Sign up: [virtualsms.io](https://virtualsms.io) (get your API key from the dashboard after signing up)
- Maven Central: [search.maven.org](https://search.maven.org/artifact/io.virtualsms/sdk)

Works with PHP, Node.js, TypeScript, Python, Ruby, .NET, Go, Rust, Swift, and Java, plus any
HTTP client and MCP-compatible AI agents such as Claude and Cursor.

## License

MIT
