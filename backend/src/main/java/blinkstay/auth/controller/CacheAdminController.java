package blinkstay.auth.controller;

import blinkstay.auth.dtos.ApiResponse;
import blinkstay.auth.dtos.CacheInfoDto;
import blinkstay.auth.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/admin/cache")
public class CacheAdminController {

    @Autowired
    private CacheService cacheService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CacheInfoDto>>> listCaches() {
        return ResponseEntity.ok(ApiResponse.of(true, "Caches", cacheService.listCaches()));
    }

    @DeleteMapping("/{cacheName}")
    public ResponseEntity<ApiResponse<String>> clearCache(@PathVariable String cacheName) {
        boolean isCleared = cacheService.clearMyCache(cacheName);

        if (!isCleared) {
            // Using the builder directly for error cases since 'of()' sets success to true
            ApiResponse<String> response = ApiResponse.<String>builder().success(false)
                    .message("Cache '" + cacheName + "' does not exist or is not registered.").data(null).build();
            return ResponseEntity.badRequest().body(response);
        }

        // Using your static helper method for success
        ApiResponse<String> response = ApiResponse.of(true, "Cache '" + cacheName + "' cleared successfully!", null);

        return ResponseEntity.ok(response);
    }
}