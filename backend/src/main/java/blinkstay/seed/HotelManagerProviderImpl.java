package blinkstay.seed;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import blinkstay.auth.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelManagerProviderImpl implements HotelManagerProvider {

	private final UserService userService;

	@Override
	public List<UUID> getHotelManagerIds() {

		return userService.getApprovedHotelManagerIds();
	}
}