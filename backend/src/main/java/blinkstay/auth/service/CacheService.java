package blinkstay.auth.service;

import blinkstay.auth.dtos.CacheInfoDto;

import java.util.List;

public interface CacheService {
    boolean clearMyCache(String cacheName);

    List<CacheInfoDto> listCaches();
}
