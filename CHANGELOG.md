# Changelog

## 2.0.0 — first REST v1-native major

**Breaking change from v1.x.** v1.x wrapped the legacy `/stubs/handler_api.php`
(sms-activate-compatible) dispatcher. v2 talks to `/api/v1/*` REST endpoints
directly — the legacy PHP dispatcher is not used by v2 at all.

- Native REST v1 client: `VirtualSmsClient(apiKey, ClientOptions)`.
- 46 methods across Activations/Orders, Rentals, Proxies, Account, Session
  (beta), Tools, and Webhooks (new in v2 — not present in v1.x at all).
- Typed exception hierarchy (`VirtualSmsException` + 6 subclasses) mapped
  from HTTP status codes.
- Client-side GET-only bounded retry (3 attempts, exponential backoff).
  Mutating calls are never auto-retried.
- Positioning change: this SDK is a native client for the VirtualSMS REST
  API v1 — not a drop-in replacement for an sms-activate-compatible client
  library. That framing belonged to v1.x only.
