package blinkstay.booking.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.booking")
@Getter
@Setter
public class BookingProperties {
	private int holdMinutes = 15;
	private int maxNights = 30;
	private int maxAdvanceDays = 365;
	private int freeCancellationHours = 48;
	private int maxPendingPerGuest = 2;
	private String currency = "INR";
	private String zone = "Asia/Kolkata";

	public ZoneId zoneId() {
		return ZoneId.of(zone);
	}
}
