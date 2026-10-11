package blinkstay.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AfterCommitCacheEvictor {
	private final CacheManager cacheManager;

	public void clear(String... cacheNames) {
		Runnable action = () -> {
			for (String name : cacheNames) {
				Cache cache = cacheManager.getCache(name);
				if (cache != null) {
					cache.clear();
				}
			}
		};

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					action.run();
				}
			});
		}
	}
}
