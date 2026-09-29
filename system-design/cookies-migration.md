# Spring Security

Why withHttpOnlyFalse()?

This is only for the CSRF cookie, not your JWT cookie.

We'll have two different cookies:

BLINKSTAY_TOKEN
HttpOnly = true
JavaScript CANNOT read it

XSRF-TOKEN
HttpOnly = false
JavaScript CAN read it

That distinction is important.

# The resulting authentication architecture

Once the controller changes are made, the backend will look like this:

```
                    LOGIN
                      │
                      ▼
             AuthenticationManager
                      │
                      ▼
                 Generate JWT
                      │
                      ▼
          Set HttpOnly Cookie
          BLINKSTAY_TOKEN
                      │
                      ▼
                 Browser
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼                       ▼
    JWT Cookie              XSRF-TOKEN
    HttpOnly=true            HttpOnly=false
          │                       │
          │                       ▼
          │                 React reads it
          │                       │
          │                       ▼
          │                X-XSRF-TOKEN
          │
          ▼
      Every API request
          │
          ▼
       JWTFilter
          │
          ▼
   Extract BLINKSTAY_TOKEN
          │
          ▼
       Validate JWT
          │
          ▼
      UserDetails
          │
          ▼
    SecurityContext
          │
          ▼
    Authorization
          │
          ▼
       Controller
```

## And CSRF protection works independently:

```
Browser
   │
   ├── Cookie: BLINKSTAY_TOKEN=...
   │
   └── Cookie: XSRF-TOKEN=abc123
                         │
                         ▼
                    React reads
                         │
                         ▼
              X-XSRF-TOKEN: abc123
                         │
                         ▼
                 Spring Security
                  validates CSRF
```

## Very important

Do not make the JWT cookie HttpOnly=false.

The security properties should eventually be approximately:

```
| Cookie            | HttpOnly | JavaScript reads it? | Purpose            |
| ----------------- | -------- | -------------------- | ------------------ |
| `BLINKSTAY_TOKEN` | `true`   | ❌                    | Authentication/JWT |
| `XSRF-TOKEN`      | `false`  | ✅                    | CSRF protection    |
```

## Why data(null) on login?

Previously:

```
POST /login
        ↓
JWT
        ↓
JSON response
        ↓
localStorage
```

We're changing it to:

```
POST /login
        ↓
JWT generated
        ↓
Set-Cookie: BLINKSTAY_TOKEN=...
        ↓
Browser stores it
```

React never receives the JWT.

That's exactly what we want.

# Late need attention ⭐

5. We also need SameSite

This is particularly important because you're using:

```
Netlify frontend
        ↓
Render backend
```

and CSRF protection.

`jakarta.servlet.http.Cookie` doesn't give us a convenient modern SameSite setter in all Servlet versions.

For the final implementation, I'd recommend using Spring's ResponseCookie, because we can explicitly configure:

```
HttpOnly
Secure
SameSite
Path
Max-Age
```

For example:

```
ResponseCookie cookie =
    ResponseCookie.from("BLINKSTAY_TOKEN", jwt)
        .httpOnly(true)
        .secure(true)
        .sameSite("None")
        .path("/")
        .maxAge(Duration.ofHours(24))
        .build();

response.addHeader(
    HttpHeaders.SET_COOKIE,
    cookie.toString()
);
```

However, don't implement this part yet.

I want to see your deployment properties and frontend setup first because SameSite=None has consequences and requires Secure=true.
