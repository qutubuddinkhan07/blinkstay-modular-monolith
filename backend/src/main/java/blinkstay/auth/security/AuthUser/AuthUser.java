package blinkstay.auth.security.AuthUser;

import java.time.Instant;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

public class AuthUser extends org.springframework.security.core.userdetails.User {
	private static final long serialVersionUID = 1L;

	private final Instant passwordChangedAt;

	public AuthUser(String username, String password, boolean enabled,
			Collection<? extends GrantedAuthority> authorities, Instant passwordChangedAt) {
		super(username, password, enabled, true, true, true, authorities);
		this.passwordChangedAt = passwordChangedAt;
	}

	public Instant getPasswordChangedAt() {
		return passwordChangedAt;
	}
}
