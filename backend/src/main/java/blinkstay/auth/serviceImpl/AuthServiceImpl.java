package blinkstay.auth.serviceImpl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.auth.entities.BlockedToken;
import blinkstay.auth.repository.BlockedTokenRepositry;
import blinkstay.auth.service.AuthService;
import blinkstay.auth.util.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
	private final AuthenticationManager authManager;

	private final JWTUtil jwtUtil;

	private final BlockedTokenRepositry blockedTokenRepositry;

	@Override
	public String authUsernameAndPasswordService(String username, String password) {
		try {
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
			Authentication authentication = authManager.authenticate(token);

			List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

			log.info("Login successful for {} with roles {}", username, roles);
			return jwtUtil.generateToken(username, roles);
		} catch (AuthenticationException e) {
			log.warn("Authentication failed for {}: {} - {}", username, e.getClass().getSimpleName(), e.getMessage());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}
	}

	@Override
	public String logoutService(HttpServletRequest request) {
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		String jwt = null;

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			jwt = authHeader.substring(7);
		}

		if (jwt == null) {
			return "No JWT found";
		}

		if (blockedTokenRepositry.existsById(jwt)) {
			return "Already Log out";
		}

		LocalDateTime expireAt = jwtUtil.extractExpiry(jwt).toInstant().atZone(ZoneId.systemDefault())
				.toLocalDateTime();

		blockedTokenRepositry.save(new BlockedToken(jwt, LocalDateTime.now(), expireAt));
		return "Log out successful";
	}

}
