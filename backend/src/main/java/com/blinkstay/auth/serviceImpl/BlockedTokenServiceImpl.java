package com.blinkstay.auth.serviceImpl;

import com.blinkstay.auth.entities.BlockedToken;
import com.blinkstay.auth.repository.BlockedTokenRepositry;
import com.blinkstay.auth.service.BlockedTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BlockedTokenServiceImpl implements BlockedTokenService {
    @Autowired
    private BlockedTokenRepositry blockedTokenRepo;

    @Autowired
    private CacheManager cacheManager;

    // Both true and false are cached. Logout writes true directly (below), so a
    // logged-out token is never answered with a stale false.
    @Override
    @Cacheable(value = "blockedTokens", key = "#token")
    public Boolean checkIfPresent(String token) {
        return blockedTokenRepo.existsByToken(token);
    }

    @Override
    public void blockToken(String token, BlockedToken blockedToken) {
        blockedTokenRepo.save(blockedToken);
        Cache cache = cacheManager.getCache("blockedTokens");
        if (cache != null) {
            cache.put(token, Boolean.TRUE);
        }
    }

}
