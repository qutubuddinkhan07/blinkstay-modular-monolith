package blinkstay.auth.config;

import blinkstay.auth.dto.TempUserRegistrationData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class SystemClassConfiguration {
    @Bean("random")
    @Lazy(value = true)
    public SecureRandom getRandom() {
        return new SecureRandom();
    }

    @Bean("otpHolder")
//    @Lazy(value = true)
    public Map<String, TempUserRegistrationData> getOtpHolder() {
        return new ConcurrentHashMap<>();
    }
}
