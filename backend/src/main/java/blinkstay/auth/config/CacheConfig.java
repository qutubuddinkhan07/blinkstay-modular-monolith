package blinkstay.auth.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

	// easy configuration
	/*
	 * public CacheManager cacheManager() { CaffeineCacheManager cacheManager = new
	 * CaffeineCacheManager("publishedListings", "listingById");
	 * 
	 * cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(500).
	 * expireAfterWrite(10, TimeUnit.MINUTES));
	 * 
	 * return cacheManager; }
	 */

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();

		// For Auth / Blocked token
		// Set TTL equal to max JWT life (e.g., 24 hours / 86400 seconds)
		cacheManager.registerCustomCache("blockedTokens",
				Caffeine.newBuilder().maximumSize(50000).expireAfterWrite(24, TimeUnit.HOURS).recordStats().build());

		// USERS SPECIFIC
		cacheManager.registerCustomCache("usersById",
				Caffeine.newBuilder().maximumSize(200).expireAfterWrite(15, TimeUnit.MINUTES).build());

		cacheManager.registerCustomCache("usersByEmail",
				Caffeine.newBuilder().maximumSize(200).expireAfterWrite(15, TimeUnit.MINUTES).build());

		// Hotel Manager IDs cache (low capacity, 60 min TTL)
		cacheManager.registerCustomCache("hotelManagerIds",
				Caffeine.newBuilder().maximumSize(10).expireAfterWrite(60, TimeUnit.MINUTES).build());

		// LISTING SPECIFIC
		// Specific configuration for individual listing lookups
		cacheManager.registerCustomCache("listingById",
				Caffeine.newBuilder().maximumSize(500).expireAfterWrite(10, TimeUnit.MINUTES).build());

		// Specific configuration for search / feed lists
		cacheManager.registerCustomCache("publishedListings",
				Caffeine.newBuilder().maximumSize(100).expireAfterWrite(5, TimeUnit.MINUTES).build());

		return cacheManager;
	}
}
