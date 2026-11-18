# 10. Testing the API (cookie + CSRF)

Swagger UI (dev profile only) cannot read an HttpOnly cookie, so use Postman or curl.

## Postman

1. Environments: `Development` (`baseUrl = http://localhost:8080`) and `Render` (`baseUrl` = the backend URL). Every request URL is `{{baseUrl}}/...`.
2. Collection scripts:
   - **After response:** copy the `XSRF-TOKEN` cookie into the variable `xsrfToken`.
   - **Before request:** add header `X-XSRF-TOKEN: {{xsrfToken}}`.
3. Postman's cookie jar keeps `BLINKSTAY_TOKEN` after login. Never paste a token by hand.

## Order to test in

| # | Request | Expect |
|---|---|---|
| 1 | `GET /api/v1/auth/csrf` | `XSRF-TOKEN` cookie appears |
| 2 | `POST /api/v1/auth/login` `{ "username": "<email>", "password": "..." }` | `data: null`, cookie `BLINKSTAY_TOKEN` (HttpOnly) |
| 3 | any protected `GET` | 200, no `Authorization` header needed |
| 4 | a protected `POST` **without** `X-XSRF-TOKEN` | 403 `CSRF_INVALID` (auth and CSRF both work) |
| 5 | the same `POST` **with** the header | reaches the controller |
| 6 | `POST /api/v1/auth/logout` | cookie cleared |
| 7 | protected `GET` again | 401 |

## curl

```bash
B=http://localhost:8080
curl -s -c jar.txt "$B/api/v1/auth/csrf" > /dev/null
XSRF=$(awk '$6=="XSRF-TOKEN"{print $7}' jar.txt)
curl -s -b jar.txt -c jar.txt -H "X-XSRF-TOKEN: $XSRF" -H 'Content-Type: application/json' \
     -d '{"username":"you@example.com","password":"..."}' "$B/api/v1/auth/login"
curl -s -b jar.txt "$B/api/v2/notifications/unread-count"
```

Time to first byte, to tell a cold start from a slow query:

```bash
for i in 1 2 3; do curl -s -o /dev/null -w "%{time_starttransfer}s\n" "$B/api/v3/listings/all"; done
```

## Feature checklists

Each design record ends with a test checklist:
[user lifecycle](architecture/user-lifecycle-events.md#9-test-checklist) ·
[notifications](architecture/notification-module.md#15-test-checklist) ·
[cancellation](architecture/b3-cancellation.md#16-test-checklist) ·
[payments](architecture/b5-payments-razorpay.md#21-test-checklist)

## Automated tests (not written yet)

- Unit tests for pure rules: `BookingRules`, `CancellationPolicy`, commission maths.
- Integration tests with Testcontainers (PostgreSQL) for the room lock, the payment processor's idempotency and the refund worker.
