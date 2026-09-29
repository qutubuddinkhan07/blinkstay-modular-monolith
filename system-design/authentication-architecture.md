Your authentication architecture now looks like this

This is what we're building:

```
                     LOGIN
                       │
                       ▼
              AuthenticationManager
                       │
                       ▼
                 UserDetails
                       │
                       ▼
                    JWTUtil
                       │
                       ▼
              ┌─────────────────┐
              │ BLINKSTAY_TOKEN │
              │   HttpOnly      │
              └─────────────────┘
                       │
                       │ browser automatically sends
                       ▼
                   JWTFilter
                       │
                       ▼
                 validate JWT
                       │
                       ▼
              extract user UUID
                       │
                       ▼
              UserDetailsService
                       │
                       ▼
              SecurityContext
                       │
                       ▼
             Controller receives
               Authentication
                       │
                       ▼
             authentication.getName()
                       │
                       ▼
                  USER UUID
```

And separately:

```
              XSRF-TOKEN
                   │
                   │ JavaScript can read
                   ▼
            X-XSRF-TOKEN header
                   │
                   ▼
             CsrfFilter
                   │
                   ▼
            POST / PUT / PATCH
            DELETE / logout
```

Spring Security specifically documents this cookie + header pattern for JavaScript applications.