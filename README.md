# VirtualSMS Java SDK

Native Java client for the [VirtualSMS](https://virtualsms.io) REST API v1 -  SMS
verification numbers, number rentals, and proxies on demand.

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

1. Get an API key at [virtualsms.io/dashboard](https://virtualsms.io/dashboard).
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

## Method coverage (46 methods)

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

## License

MIT
