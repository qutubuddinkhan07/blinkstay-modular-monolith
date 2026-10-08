# Adding Caching

1. First add Spring Cache

In pom.xml:

```java
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

For your current BlinkStay project, I'd recommend Caffeine for now because it is simple and fast for a single application instance:

```java
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

Later, if BlinkStay runs on multiple backend instances, you can move to Redis.

2. Enable caching

Add @EnableCaching to one of your configuration classes.

For example:

```java
@Configuration
@EnableCaching
public class CacheConfig {
}
```

You can also put @EnableCaching on your existing Spring configuration class.

3. Configure Caffeine

I'd recommend explicitly configuring it rather than using the default cache.

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {

        CaffeineCacheManager cacheManager =
                new CaffeineCacheManager(
                        "publishedListings",
                        "listingById"
                );

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                );

        return cacheManager;
    }
}
```

So your cache will roughly behave like:

```
publishedListings
        ↓
maximum 500 entries
        ↓
each entry expires after 10 minutes
```

4. The basic caching syntax

There are three annotations you should learn first.

```java
@Cacheable
```

Used for read operations.

```java
@Cacheable("listingById")
public ListingDetailsResponseDto getListingById(UUID listingId) {
    ...
}
```

First request:

```
GET /listings/123

Cache?
  ↓
MISS
  ↓
Database
  ↓
Response
  ↓
Store in cache
```

Second request:

```
GET /listings/123

Cache?
  ↓
HIT
  ↓
Return cached response
```

The database isn't queried again.

## `@CacheEvict`

Used when data changes.

```java
@CacheEvict(
    cacheNames = "listingById",
    key = "#listingId"
)
public String updateListing(
        UUID managerId,
        UUID listingId,
        AddListingDto dto) {

    ...
}
```

This means:

```
update listing 123
       ↓
database updated
       ↓
remove cache["123"]
```

The next GET will query the database again and rebuild the cache.

## `@CachePut`

This is used when you want to update the cache with the method's returned value.

```java
@CachePut(
    cacheNames = "listingById",
    key = "#listingId"
)
public ListingDetailsResponseDto updateListing(...) {
    ...
}
```

You probably don't need @CachePut yet. For BlinkStay, @CacheEvict is simpler and safer.

5. Where I would put caching in YOUR service

Looking at your service, these are the important read methods:

```java
getListingById()
getAllListingsByManager()
getListingsByManagerId()
getPublishedListings()
```

But I would <b>not cache everything immediately</b>.

I'd start with:

```java
getListingById()
getPublishedListings()
```

because these are likely to be called frequently by users browsing BlinkStay.

## The 3 annotations to remember

Think of Spring Cache like this:

```
| Annotation    | Purpose                            | Example                                |
| ------------- | ---------------------------------- | -------------------------------------- |
| `@Cacheable`  | Read from cache / create cache     | `getListingById()`                     |
| `@CacheEvict` | Remove stale cache                 | `updateListing()`                      |
| `@CachePut`   | Execute method + update cache      | Less useful for you right now          |
| `@Caching`    | Multiple cache operations together | `updateListing()` + published listings |
```

The most important pattern for BlinkStay is:

```
                 GET
                  │
                  ▼
          ┌───────────────┐
          │ @Cacheable    │
          └───────┬───────┘
                  │
          Cache HIT? ────── YES ──► Return
                  │
                  NO
                  │
                  ▼
              Database
                  │
                  ▼
              Store Cache


       CREATE / UPDATE / DELETE / PUBLISH
                    │
                    ▼
             @CacheEvict
                    │
                    ▼
             Remove stale data
```

# Key Strategy for Listings
Key Caching Strategy
1. getListingById: `@Cacheable(value = "listingById", key = "#listingId")` — Caches individual listing details.

2. getPublishedListings: `@Cacheable(value = "publishedListings", key = "{#page, #size, #sortBy, #direction, #country}")` — Caches paged listing feeds by query params.

3. updateListing, listingPublishService, addListingImages, deleteListingImage: Use `@CacheEvict` to clear listingById for that specific ID and clear publishedListings (using allEntries = true).

# Key Strategy for Users
Where to Use Caching & Eviction
1. getUserById(UUID userId) $\rightarrow$ `@Cacheable`

Why: High-frequency call triggered across services to load current user profiles or manager metadata.

2. getUserByEmail(String userEmail) $\rightarrow$ `@Cacheable`

Why: Heavily called during login/authentication filter checks.

3. getApprovedHotelManagerIds() $\rightarrow$ `@Cacheable`

Why: System-wide check returning list of active manager IDs; changes infrequently.

4. deleteUserByEmail, updateProfileImage, replaceProfileImage, deleteProfileImage $\rightarrow$ `@CacheEvict`

Why: Must invalidate the cached usersById and usersByEmail entries whenever user state or profile picture changes.

# Key Strategy for Auth & Blocked Token Repository
## Where & How to Implement Caching

1. BlockedTokenServiceImpl.checkIfPresent(String token) $\rightarrow$ @Cacheable

- Why: Executed on every request by your Security Filter. Caching standardizes lookup times to sub-millisecond in-memory speeds.
- Condition: Only cache true results using unless = "#result == false". If a token is not blocked yet, caching false would cause subsequent requests to fail to detect a newly invalidated token during logout.

2. AuthServiceImpl.logoutService(...) $\rightarrow$ @CachePut or @CacheEvict

- Why: When a user logs out, instantly place the token into the blocked cache or evict stale entries so checkIfPresent() immediately returns true.