package blinkstay.common.directory;

import java.time.LocalDate;

public record HostSummary(String name, String businessName, String photoUrl, String about, LocalDate memberSince) {
}
