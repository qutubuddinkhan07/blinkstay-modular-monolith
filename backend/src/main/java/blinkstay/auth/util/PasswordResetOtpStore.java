package blinkstay.auth.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component
public class PasswordResetOtpStore {

	public static class Entry {
		private final String otpHash;
		private final Instant createdAt = Instant.now();
		private final AtomicInteger attempts = new AtomicInteger();

		public Entry(String otpHash) {
			this.otpHash = otpHash;
		}

		public String otpHash() {
			return otpHash;
		}

		public Instant createdAt() {
			return createdAt;
		}

		public AtomicInteger attempts() {
			return attempts;
		}
	}

	// entries disappear by themselves 10 minutes after they were written
	private final Cache<String, Entry> cache = Caffeine.newBuilder().maximumSize(10_000)
			.expireAfterWrite(Duration.ofMinutes(10)).build();

	public Entry get(String key) {
		return cache.getIfPresent(key);
	}

	public void put(String key, Entry entry) {
		cache.put(key, entry);
	}

	public void remove(String key) {
		cache.invalidate(key);
	}
}