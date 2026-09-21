package blinkstay.auth.service;

public interface BlockedTokenService {
	Boolean checkIfPresent(String token);
}
