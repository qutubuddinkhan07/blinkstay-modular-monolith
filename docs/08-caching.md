# 8. Caching

Spring Cache with **Caffeine** (in process), configured in `auth/config/CacheConfig`. Each cache is registered by name with its own size and expiry.

| Cache | Holds | Max entries | Expires after write | Cleared when |
|---|---|---|---|---|
| `listingById` | listing detail | 500 | 10 min | listing, room, host details or rating changes |
| `publishedListings` | one page of the public feed (key: page, size, sort, direction, filters) | 100 | 5 min | any listing or rating change (whole cache) |
| `usersById` | user DTO by id | 200 | 15 min | user changes |
| `usersByEmail` | user DTO by email | 200 | 15 min | user changes |
| `authUsers` | snapshot for the JWT filter: id, roles, enabled, `passwordChangedAt` | 5,000 | 60 s | block, unblock, delete, restore, role change, password change or reset (whole cache) |
| `hotelManagerIds` | ids of approved hosts (seeding) | 10 | 60 min | host approval |
| `blockedTokens` | "is this token logged out?" | 50,000 | 24 h (token lifetime) | logout writes `true` directly |

Separate Caffeine stores, not Spring caches: the registration `otpHolder` (10 min, 1,000 entries) and `PasswordResetOtpStore` (10 min, 10,000 entries).

## Keeping it correct

- **Evict after the commit.** `AfterCommitCacheEvictor.clear("listingById", ...)` registers a callback that clears the named caches in `afterCommit()`. Clearing earlier would let a parallel request re-cache the old row. On rollback nothing is cleared, which is right because nothing changed. It clears whole caches, and does nothing if no transaction is active, so call it only inside one.
- **TTL is the safety net** for a forgotten eviction.
- **Not cached on purpose:** availability, "free tonight", the host calendar, anything about money.
- **Pitfalls met:** self-invocation skips `@Cacheable` / `@Transactional` (call across beans); a missing `@EventListener` once meant the `HostDetailsChangedEvent` eviction never ran; an unknown cache name creates an unbounded cache silently, because the manager is in dynamic mode (fix: fixed names with `setCacheNames`, and validate names in `DELETE /api/v2/admin/cache/{name}`).

## Checking it

Call an endpoint twice with SQL logging on: the second call runs no query. Change the data, call again: one query. Live numbers from production: a cached feed page ≈ 0.3 s, uncached ≈ 2 s (the database is in another city).

## Limit

The caches live in one JVM. With two instances, an eviction on one does not reach the other (stale listings; a blocked user could stay in for up to 60 s on the other server). Move to Redis before scaling out ([09](09-scaling-and-microservices.md)).
