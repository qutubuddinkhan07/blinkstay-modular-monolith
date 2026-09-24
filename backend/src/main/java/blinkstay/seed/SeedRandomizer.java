package blinkstay.seed;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class SeedRandomizer {

	private final Random random = new SecureRandom();

	public UUID randomManager(List<UUID> managers) {

		if (managers == null || managers.isEmpty()) {

			throw new IllegalStateException("No HOTEL_MANAGER users found. "
					+ "Create at least one approved hotel manager " + "before running listing seed.");
		}

		return managers.get(random.nextInt(managers.size()));
	}
}