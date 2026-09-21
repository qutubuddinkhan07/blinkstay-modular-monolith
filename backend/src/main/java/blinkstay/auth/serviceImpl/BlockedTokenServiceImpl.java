package blinkstay.auth.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import blinkstay.auth.repository.BlockedTokenRepositry;
import blinkstay.auth.service.BlockedTokenService;

@Service
public class BlockedTokenServiceImpl implements BlockedTokenService {
	@Autowired
	private BlockedTokenRepositry blockedTokenRepo;

	@Override
	public Boolean checkIfPresent(String token) {
		return blockedTokenRepo.existsByToken(token);
	}

}
