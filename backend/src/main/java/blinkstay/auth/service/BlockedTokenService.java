package blinkstay.auth.service;

import blinkstay.auth.entities.BlockedToken;

public interface BlockedTokenService {
	Boolean checkIfPresent(String token);

	void blockToken(String token, BlockedToken blockedToken);
}
