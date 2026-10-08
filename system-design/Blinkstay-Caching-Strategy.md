# BlinkStay Caching Strategy & Monitoring Reference

This guide covers the caching architecture, configurations, invalidation patterns, and Actuator metrics monitoring for the **BlinkStay** backend using Spring Boot Cache and Caffeine.

---

## 1. Core Architecture Overview

- **`spring-boot-starter-cache`**: Provides the declarative abstraction layer (`@Cacheable`, `@CacheEvict`, `@CachePut`).
- **`caffeine`**: Serves as the high-performance in-memory caching engine that executes eviction policies, memory capping, and time-to-live (TTL) expiry.

### Key Comparison

| Feature / Aspect        | `spring-boot-starter-cache`                                                         | `caffeine`                                                                   |
| ----------------------- | ----------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| **Role**                | Caching Abstraction Layer                                                           | Caching Implementation / Engine                                              |
| **Primary Job**         | Intercepts method calls using Spring AOP (`@Cacheable`) and manages cache lifecycle | Holds the actual data in memory with advanced eviction and expiry algorithms |
| **Eviction Strategies** | None on its own (relies on the backing cache implementation)                        | Advanced (W-TinyLFU algorithm, size-based, time-to-live, time-to-idle)       |
| **Storage Location**    | N/A (acts as a wrapper)                                                             | In-Memory (JVM Heap)                                                         |
| **Configuration**       | Configured via `@EnableCaching` and standard Spring properties                      | Configured via `caffeine.spec` or Java bean definitions                      |
| **Standalone Usage**    | Cannot function without a backing provider (defaults to simple `ConcurrentHashMap`) | Can be used standalone in pure Java applications without Spring              |

---

## 2. Interactive Architecture Visual

The diagram below illustrates the request lifecycle flow across the AOP Proxy, Caffeine cache, and Database backing store. Toggle between **Cache Miss** and **Cache Hit** modes, and click any node to inspect its role.

---

## 3. Recommended Cache Configurations

### Option A: Shared Baseline Configuration (Default)

Applies a single global specification across all declared caches (`listingById`, `publishedListings`).

```java
package com.blinkstay.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "publishedListings",
                "listingById"
        );

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats() // Required for Micrometer/Actuator metrics
        );

        return cacheManager;
    }
}
```

### Option B: Per-Cache Custom Configurations (Best Practice)

Allows setting individual sizes and expiry policies tailored to specific data access patterns.

```java
package com.blinkstay.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 1. Single listing lookup: High volume, longer retention
        cacheManager.registerCustomCache("listingById",
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // 2. Search/Feed query pages: Dynamic, shorter retention
        cacheManager.registerCustomCache("publishedListings",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        return cacheManager;
    }
}
```

---

## 4. Cache Eviction & Service Integration

To prevent stale data when hosts edit or delete listings, invoke `@CacheEvict` in write operations.

```java
package com.blinkstay.service;

import com.blinkstay.dto.ListingDto;
import com.blinkstay.dto.UpdateListingDto;
import com.blinkstay.repository.ListingRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ListingService {

    private final ListingRepository listingRepository;

    public ListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    // Read: Cached until TTL or explicitly evicted
    @Cacheable(value = "listingById", key = "#id")
    public ListingDto getListingById(Long id) {
        return listingRepository.findById(id)
                .map(ListingDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
    }

    // Write: Clears the specific listing cache and invalidates stale search feeds
    @CacheEvict(value = "listingById", key = "#id")
    @CacheEvict(value = "publishedListings", allEntries = true)
    public ListingDto updateListing(Long id, UpdateListingDto dto) {
        // ... Database update logic ...
        return updatedListing;
    }
}
```

---

## 5. Monitoring & Actuator Integration

### Step 1: Maven Dependencies (`pom.xml`)

```xml
<dependencies>
    <!-- Spring Cache Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- Caffeine Engine -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- Spring Boot Actuator for Metrics -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Prometheus Export (Optional) -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

### Step 2: Configuration (`application.properties`)

```properties
# Expose actuator REST endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# Enable cache health checks
management.health.caches.enabled=true
```

### Step 3: Actuator Metrics Reference

| API Target                   | HTTP Request                                                                                  |
| ---------------------------- | --------------------------------------------------------------------------------------------- |
| All Registered Cache Metrics | `GET http://localhost:8080/actuator/metrics/cache.gets`                                       |
| Cache Hits (`listingById`)   | `GET http://localhost:8080/actuator/metrics/cache.gets?tag=cache:listingById&tag=result:hit`  |
| Cache Misses (`listingById`) | `GET http://localhost:8080/actuator/metrics/cache.gets?tag=cache:listingById&tag=result:miss` |
| Eviction Count               | `GET http://localhost:8080/actuator/metrics/cache.evictions?tag=cache:listingById`            |
| Current Entries Count        | `GET http://localhost:8080/actuator/metrics/cache.size?tag=cache:listingById`                 |
| JVM Heap Memory Usage        | `GET http://localhost:8080/actuator/metrics/jvm.memory.used`                                  |
| Prometheus Metrics Stream    | `GET http://localhost:8080/actuator/prometheus`                                               |

---

## 6. How They Work Together

In a standard Spring Boot setup, you include both dependencies:

**`spring-boot-starter-cache`** provides the declarative annotations:

```java
@Service
public class ListingService {
    @Cacheable(value = "listings", key = "#id")
    public ListingDto getListingById(Long id) {
        // Database lookup happens only on cache miss
        return listingRepository.findById(id).orElseThrow();
    }
}
```

**`caffeine`** provides the rules in `application.properties`:

```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=10m
```

### Recommended Uses

| Dependency                  | Best Used For                                                                                                                                                                                                                                                                          |
| --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `spring-boot-starter-cache` | • Enabling annotation-driven caching in Spring applications (`@Cacheable`).<br>• Decoupling your code from specific caching backends (allowing easy switching between Caffeine, Redis, Hazelcast, or Ehcache).                                                                         |
| `caffeine`                  | • High-performance, single-instance in-memory caching for applications like BlinkStay.<br>• Setting TTL (Time-To-Live), maximum cache sizes, or auto-refreshing cache entries.<br>• Scenarios where network latency to a remote cache server (like Redis) is unnecessary or undesired. |

---

## Summary Checklist

- [x] Ensure `.recordStats()` is invoked on every Caffeine builder instance.
- [x] Pre-define or dynamically register cache names inside `CaffeineCacheManager`.
- [x] Pair mutation methods with `@CacheEvict(allEntries = true)` on listing list/search feeds.
- [x] Verify endpoints via `/actuator/metrics/cache.gets`.
