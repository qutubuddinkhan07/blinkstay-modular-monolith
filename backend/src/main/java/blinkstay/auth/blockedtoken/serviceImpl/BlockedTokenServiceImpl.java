package blinkstay.auth.blockedtoken.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import blinkstay.auth.blockedtoken.service.BlockedTokenService;
import blinkstay.auth.repository.BlockedTokenRepositry;

@Service
public class BlockedTokenServiceImpl implements BlockedTokenService {
	@Autowired
	private BlockedTokenRepositry blockedTokenRepo;

	@Override
	public Boolean checkIfPresent(String username) {
		return blockedTokenRepo.existsById(username);
	}

}
