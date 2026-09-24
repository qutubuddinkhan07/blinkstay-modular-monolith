package blinkstay.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(10)
public class SeedDataInitializer implements CommandLineRunner {

	private final SeedDataService seedDataService;

	@Override
	public void run(String... args) {

		seedDataService.seed();
	}
}
