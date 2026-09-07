# Live chat

The admin console connects to `/api/v1/notifications/ws` through the BFF using
its existing session cookie. The gateway relays the access token during the
WebSocket upgrade. The backend rejects anonymous, unknown, and expired users;
connections close when their access token expires and the client reconnects.
No access token or recipient identifier is accepted from browser socket messages.

The connection carries JSON `{ "type": "message", "data": { "conversationId": "…" } }`
events, plus `notification`, `connected`, and `heartbeat` events. The console
refetches the affected authorized API queries. Message creation, deletion, read
markers, and closure still use the existing REST endpoints and transactions.
Events are emitted after commit; all participant tabs, including the sender's,
receive changes. Notifications still respect muting. SSE remains available for
other clients during migration; the admin console uses WebSocket exclusively.

## Deployment

Deploy/restart the backend, gateway, and admin console together.

- Gateway: set `BACKEND_WS_URI` to the backend's WebSocket address, for example
  `ws://backend:8080` on an internal network or `wss://api.example.com` over TLS.
  Default: `ws://localhost:8080`. This is separate from `BACKEND_URI`.
- Backend: set `APP_WEBSOCKET_ALLOWED_ORIGINS` to the public gateway origin, for
  example `https://career.example.com`. Multiple origins can be comma separated.
  Default: `http://localhost:8090`. Do not use a wildcard.
- The reverse proxy must allow WebSocket upgrades and an idle timeout longer
  than the 25-second heartbeat interval.

The live registry is in memory and supports one backend instance. A shared broker
is required before horizontal scaling. History is always persisted in the
database; the client reloads after reconnection to recover missed events.

## Checks

`./gradlew test --tests '*LiveWebSocketHandlerTest' --tests '*NotificationStreamServiceTest' --tests '*SecurityRulesTest'`

For an end-to-end smoke check, sign in as two participants in separate browser
profiles through the gateway. Open the same conversation and verify send, read,
delete, close, reconnect, and a muted recipient. Confirm the upgrade returns 101
and an unrelated account receives no conversation events.

References: [Spring WebSocket API](https://docs.spring.io/spring-framework/reference/web/websocket/server.html)
and [Spring Cloud Gateway routing](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/).
