package blinkstay.auth.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import blinkstay.auth.service.CacheService;

@Service
public class CacheServiceImpl implements CacheService {
	@Autowired
	private CacheManager cacheManager;

	@Override
	public boolean clearMyCache(String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);

		if (cache == null) {
			return false;
		}

		cache.clear(); // Clears the cache contents
		return true;
	}
}
