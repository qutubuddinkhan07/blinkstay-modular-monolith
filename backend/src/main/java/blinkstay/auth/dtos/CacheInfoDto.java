package blinkstay.auth.dtos;

public record CacheInfoDto(String name, long estimatedSize, long hits, long misses, double hitRate, long evictions) {
}
