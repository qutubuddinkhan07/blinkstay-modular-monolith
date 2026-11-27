package com.blinkstay.auth.service;

import com.blinkstay.auth.entities.BlockedToken;

public interface BlockedTokenService {
	Boolean checkIfPresent(String token);

	void blockToken(String token, BlockedToken blockedToken);
}
