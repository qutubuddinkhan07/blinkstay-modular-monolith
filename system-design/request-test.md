# Testing request using cookies

# Swagger Limitation
But there is a Swagger limitation

Even after this configuration, Swagger UI isn't going to magically give your JavaScript access to the HttpOnly cookie.

That's actually the whole point of:

HttpOnly=true

The browser owns the cookie.

So don't worry if Swagger's authentication button doesn't behave exactly like the old Bearer-token flow.

For your first test, use Postman.

# Using postman guide

4. Test login in Postman

Start your Spring Boot backend.

Then:

```
POST http://localhost:8080/api/v1/auth/login
```

Body:

```
{
    "username": "your-email-or-username",
    "password": "your-password"
}
```

Use:
```
Body → raw → JSON
```


You should get something similar to:

```
{
    "success": true,
    "message": "Login Successful!",
    "data": null
}
```

The important part isn't the JSON.

Go to Postman's Cookies section for:

```
localhost
```

You should see:
```
BLINKSTAY_TOKEN
```

with something approximately like:

```
Name: BLINKSTAY_TOKEN
Value: eyJhbGciOiJIUzI1Ni...
HttpOnly: true
Secure: false
Path: /
Max-Age: 86400
```

If you see that, your login cookie is being created correctly.

5. Test an authenticated GET request

Now call an endpoint that requires authentication.

For example, one of your protected endpoints:

```
GET http://localhost:8080/api/v3/listings/...
```

or another endpoint that requires:

```
.authenticated()
```

You do not manually add:

```
Authorization: Bearer ...
```

Instead Postman should automatically send:

```
Cookie: BLINKSTAY_TOKEN=eyJ...
```

Your JWTFilter should then do:

```
Request
   ↓
BLINKSTAY_TOKEN cookie
   ↓
JWTFilter
   ↓
extract JWT
   ↓
validate JWT
   ↓
extract user UUID
   ↓
load UserDetails
   ↓
SecurityContext
   ↓
Spring Security authorization
   ↓
Controller
```

You can temporarily log:

```
System.out.println("JWT FILTER");
System.out.println("Request: " + request.getRequestURI());
```

but don't print the actual JWT in production logs.

6. Test that authentication actually works

This is particularly important because you mentioned:

I didn't do much just extracted the id from the authentication principal

That's perfectly fine.

For example, if you have:

```
Authentication authentication
```

you can get:

```
String userId = authentication.getName();
```

because your JWT filter creates:

```
UsernamePasswordAuthenticationToken authToken =
        new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
```

and your UserDetails has:

```
User.withUsername(user.getId().toString())
```

Therefore:

```
authentication.getName()
```

will give you:
```
user UUID
```

You don't need to extract the JWT manually inside every controller.

That's actually one of the benefits of your current design.

7. Now test CSRF

This is the part that is important.

With:

```
.csrf(csrf -> csrf
    .csrfTokenRepository(
        CookieCsrfTokenRepository.withHttpOnlyFalse()
    )
)
```

Spring Security will use a CSRF cookie:

```
XSRF-TOKEN
```

and expect the token in:

```
X-XSRF-TOKEN: <token>
```

for state-changing requests.

So your authentication cookies will conceptually be:

```
BLINKSTAY_TOKEN
        ↓
Authentication
        ↓
"Who are you?"

XSRF-TOKEN
        ↓
CSRF protection
        ↓
"Did this request intentionally come from our frontend?"
```

They serve completely different purposes.

8. You need a CSRF endpoint

For your React application, I recommend having a simple endpoint:

```
@GetMapping("/csrf")
public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
    return ResponseEntity.ok().build();
}
```

For example:

```java
package blinkstay.auth.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class CsrfController {

    @GetMapping("/csrf")
    public void csrf(CsrfToken csrfToken) {
        // Accessing the token causes Spring Security
        // to generate/send the CSRF cookie.
    }
}
```

You can also put this endpoint in your existing AuthController; I prefer a separate small controller because it keeps responsibilities clear.

Then make sure:

```
.requestMatchers(
    "/api/v1/auth/login",
    "/api/v1/auth/csrf",
    ...
).permitAll()
```

The endpoint itself doesn't need authentication.

9. Test CSRF in Postman

First:

```
GET http://localhost:8080/api/v1/auth/csrf
```

Check Postman's cookies.

You should get:

```
XSRF-TOKEN
```

Then login:

```
POST http://localhost:8080/api/v1/auth/login
```

Postman should now have:

```
BLINKSTAY_TOKEN
XSRF-TOKEN
```

10. Test a protected POST without CSRF

Now intentionally make a protected POST request without:

```
X-XSRF-TOKEN
```

For example:

```
POST http://localhost:8080/api/v3/...
```

while the authentication cookie is present.

You should get something like:

```
403 Forbidden
```

That's actually a good sign.

It means:

```
JWT authentication → working
CSRF protection → working
```

but the request doesn't contain the CSRF token.

11. Then send the CSRF header

Take the value of:

```
XSRF-TOKEN
```

from Postman's cookie manager.

Then add:

```
X-XSRF-TOKEN: <value-of-XSRF-TOKEN>
```

to the request.

Keep the authentication cookie:

```
Cookie: BLINKSTAY_TOKEN=<JWT>
```

Now your request should pass CSRF and reach your controller.

12. Logout test

Your logout is:

```
POST /api/v1/auth/logout
```

and you currently have:

```
@PreAuthorize("isAuthenticated()")
```

That's fine.

The complete request needs:

```
BLINKSTAY_TOKEN
XSRF-TOKEN
X-XSRF-TOKEN
```

The flow is:

```
POST /logout
       │
       ├── BLINKSTAY_TOKEN
       │       ↓
       │    JWTFilter
       │       ↓
       │    authenticated
       │
       └── X-XSRF-TOKEN
               ↓
           CSRF filter
               ↓
             valid
               ↓
          logoutService()
               ↓
        blocked-token table
               ↓
        expire BLINKSTAY_TOKEN
```

After logout, Postman's cookie should show that BLINKSTAY_TOKEN has been removed/expired.

Then try an authenticated endpoint again.

You should receive:

```
401 Unauthorized
```

Your testing order

Don't test everything at once. Do it in this exact order:

```
1. Start backend
       ↓
2. GET /api/v1/auth/csrf
       ↓
3. Confirm XSRF-TOKEN exists
       ↓
4. POST /api/v1/auth/login
       ↓
5. Confirm BLINKSTAY_TOKEN exists
       ↓
6. GET protected endpoint
       ↓
7. Confirm authentication works
       ↓
8. POST protected endpoint WITHOUT X-XSRF-TOKEN
       ↓
9. Confirm 403
       ↓
10. POST protected endpoint WITH X-XSRF-TOKEN
       ↓
11. Confirm request succeeds
       ↓
12. POST /api/v1/auth/logout
       ↓
13. Confirm BLINKSTAY_TOKEN is cleared
       ↓
14. Try protected endpoint again
       ↓
15. Confirm 401
```

