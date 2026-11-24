package blinkstay.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * BackendApplication has @EnableCaching, which needs a CacheManager. Slice tests
 * (@DataJpaTest, later @WebMvcTest) don't load cache auto-configuration, so this
 * gives them one that never caches.
 */
@TestConfiguration(proxyBeanMethods = false)
public class NoOpCacheConfig {

    @Bean
    CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}