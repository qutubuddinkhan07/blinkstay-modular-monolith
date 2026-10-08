package blinkstay.auth.serviceImpl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.auth.entities.BlockedToken;
import blinkstay.auth.repository.BlockedTokenRepositry;
import blinkstay.auth.service.AuthService;
import blinkstay.auth.util.JWTUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
	@Value("${app.cookie.name}")
	private String authCookieName;

	@Value("${app.cookie.max-age}")
	private int cookieMaxAge;

	@Value("${app.cookie.secure}")
	private boolean cookieSecure;

	@Value("${app.cookie.same-site}")
	private String cookieSameSite;

	private final AuthenticationManager authManager;

	private final JWTUtil jwtUtil;

	private final BlockedTokenRepositry blockedTokenRepositry;

	@Override
	public void authUsernameAndPasswordService(String username, String password, HttpServletResponse response) {
		try {
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
			Authentication authentication = authManager.authenticate(token);

			// Retrieve the authenticated UserDetails (its username is now
			// user.getId().toString())
			UserDetails userDetails = (UserDetails) authentication.getPrincipal();
			String userIdStr = userDetails.getUsername();

			List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
					.filter(auth -> !auth.startsWith("FACTOR_")).toList();

			log.info("Login successful for userId {} with roles {}", userIdStr, roles);

			String jwt = jwtUtil.generateToken(userIdStr, roles);

			ResponseCookie authCookie = ResponseCookie.from(authCookieName, jwt).httpOnly(true).secure(cookieSecure)
					.sameSite(cookieSameSite).path("/").maxAge(Duration.ofSeconds(cookieMaxAge)).build();

			response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
		} catch (AuthenticationException e) {
			log.warn("Authentication failed for {}: {} - {}", username, e.getClass().getSimpleName(), e.getMessage());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}
	}

	@Override
	public String logoutService(HttpServletRequest request, HttpServletResponse response) {
		String jwt = extractTokenFromCookies(request);

		if (jwt == null) {
			clearAuthCookie(response);
			return "No JWT found";
		}

		if (blockedTokenRepositry.existsByToken(jwt)) {
			clearAuthCookie(response);
			return "Already Log out";
		}

		LocalDateTime expireAt = jwtUtil.extractExpiry(jwt).toInstant().atZone(ZoneId.systemDefault())
				.toLocalDateTime();

		BlockedToken token = BlockedToken.builder().token(jwt).blockedAt(LocalDateTime.now()).expiresAt(expireAt)
				.build();

		blockedTokenRepositry.save(token);

		// Helper call to populate the cache immediately on logout
		cacheBlockedToken(jwt);

		clearAuthCookie(response);

		return "Logout successful";
	}

	// Helper method to update cache on logout
	@CachePut(value = "blockedTokens", key = "#token")
	public Boolean cacheBlockedToken(String token) {
		return true;
	}

	private String extractTokenFromCookies(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();

		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (authCookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return null;
	}

	private void clearAuthCookie(HttpServletResponse response) {
		ResponseCookie expiredCookie = ResponseCookie.from(authCookieName, "").httpOnly(true).secure(cookieSecure)
				.sameSite(cookieSameSite).path("/").maxAge(Duration.ZERO).build();

		response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
	}

}
