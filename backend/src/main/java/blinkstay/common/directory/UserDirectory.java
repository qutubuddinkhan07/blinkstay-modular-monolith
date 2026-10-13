package blinkstay.common.directory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDirectory {
	Optional<UserContact> findContact(UUID userId); // works for blocked users too

	List<UserContact> findActiveAdmins();
}
