package blinkstay.auth.runners;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PasswordGenerator {
	private final PasswordEncoder passwordEncoder;

	public String generatePassword(String password) {
		String pass = passwordEncoder.encode(password);
		return pass;
	}
}
