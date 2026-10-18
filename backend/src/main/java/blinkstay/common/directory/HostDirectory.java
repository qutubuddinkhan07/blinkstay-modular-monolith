package blinkstay.common.directory;

import java.util.Optional;
import java.util.UUID;

public interface HostDirectory {
	/** Empty when the user no longer exists or has deleted their account. */
	Optional<HostSummary> findHost(UUID userId);
}
