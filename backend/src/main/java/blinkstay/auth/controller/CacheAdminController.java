package blinkstay.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.auth.dtos.ApiResponse;
import blinkstay.auth.service.CacheService;

@RestController
@RequestMapping("/api/v2/admin/cache")
public class CacheAdminController {

	@Autowired
	private CacheService cacheService;

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