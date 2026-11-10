package blinkstay.auth.config;

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

        // Blocked (logged-out) tokens: same lifetime as a JWT
        cacheManager.registerCustomCache("blockedTokens", Caffeine.newBuilder().maximumSize(50000)
                .expireAfterWrite(24, TimeUnit.HOURS).recordStats().build());

        cacheManager.registerCustomCache("usersById", Caffeine.newBuilder().maximumSize(200)
                .expireAfterWrite(15, TimeUnit.MINUTES).recordStats().build());

        cacheManager.registerCustomCache("usersByEmail", Caffeine.newBuilder().maximumSize(200)
                .expireAfterWrite(15, TimeUnit.MINUTES).recordStats().build());

        cacheManager.registerCustomCache("hotelManagerIds", Caffeine.newBuilder().maximumSize(10)
                .expireAfterWrite(60, TimeUnit.MINUTES).recordStats().build());

        cacheManager.registerCustomCache("listingById", Caffeine.newBuilder().maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES).recordStats().build());

        cacheManager.registerCustomCache("publishedListings", Caffeine.newBuilder().maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES).recordStats().build());

        return cacheManager;
    }
}