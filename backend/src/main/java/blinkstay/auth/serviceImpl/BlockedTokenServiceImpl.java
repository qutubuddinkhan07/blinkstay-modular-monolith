package blinkstay.auth.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import blinkstay.auth.entities.BlockedToken;
import blinkstay.auth.repository.BlockedTokenRepositry;
import blinkstay.auth.service.BlockedTokenService;

@Service
public class BlockedTokenServiceImpl implements BlockedTokenService {
	@Autowired
	private BlockedTokenRepositry blockedTokenRepo;

	// Cache ONLY blocked tokens (where result == true)
	// If false, don't cache so future logouts are recognized immediately
	// Cache all check results (both true and false)
	@Override
	@Cacheable(value = "blockedTokens", key = "#token", unless = "!#result")
	public Boolean checkIfPresent(String token) {
		return blockedTokenRepo.existsByToken(token);
	}

	// Evict or update the cache key immediately upon logout
	@Override
	@CacheEvict(value = "blockedTokens", key = "#token")
	public void blockToken(String token, BlockedToken blockedToken) {
		blockedTokenRepo.save(blockedToken);
	}

}
