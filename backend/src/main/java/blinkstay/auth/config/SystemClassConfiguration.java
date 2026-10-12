package blinkstay.auth.config;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.github.benmanes.caffeine.cache.Caffeine;

import blinkstay.auth.dtos.TempUserRegistrationData;

@Configuration
public class SystemClassConfiguration {
	@Bean("random")
	@Lazy(value = true)
	public SecureRandom getRandom() {
		return new SecureRandom();
	}

	@Bean("otpHolder")
	public Map<String, TempUserRegistrationData> otpHolder() {
		return Caffeine.newBuilder().maximumSize(1_000).expireAfterWrite(Duration.ofMinutes(10))
				.<String, TempUserRegistrationData>build().asMap();
	}
}
