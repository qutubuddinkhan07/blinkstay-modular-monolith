package blinkstay.auth.serviceImpl;

import blinkstay.auth.dtos.CacheInfoDto;
import blinkstay.auth.service.CacheService;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {
    private final CacheManager cacheManager;

    @Override
    public boolean clearMyCache(String cacheName) {
        // Check the name FIRST: asking a Caffeine manager for an unknown name would
        // silently create an empty cache and report success.
        if (!cacheManager.getCacheNames().contains(cacheName)) {
            return false;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return false;
        }

        cache.clear();
        return true;
    }

    @Override
    public List<CacheInfoDto> listCaches() {
        return cacheManager.getCacheNames().stream().sorted().map(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache instanceof CaffeineCache caffeine) {
                var nativeCache = caffeine.getNativeCache();
                CacheStats s = nativeCache.stats();
                return new CacheInfoDto(name, nativeCache.estimatedSize(), s.hitCount(), s.missCount(), s.hitRate(),
                        s.evictionCount());
            }
            return new CacheInfoDto(name, -1, 0, 0, 0, 0);
        }).toList();
    }
}
