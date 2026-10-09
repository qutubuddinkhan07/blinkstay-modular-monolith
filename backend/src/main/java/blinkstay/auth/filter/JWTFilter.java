package blinkstay.auth.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import blinkstay.auth.service.BlockedTokenService;
import blinkstay.auth.serviceImpl.CustomUserDetailsDaoService;
import blinkstay.auth.util.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

	/**
	 * Read by the entry point so the frontend can tell WHY the request was
	 * rejected.
	 */
	public static final String AUTH_ERROR_ATTRIBUTE = "blinkstay.auth.error";

	@Value("${app.cookie.name}")
	private String authCookieName;

	// These must match how the login code sets the cookie, or the browser won't
	// overwrite it
	@Value("${app.cookie.secure:false}")
	private boolean cookieSecure;

	@Value("${app.cookie.same-site:Lax}")
	private String cookieSameSite;

	private final JWTUtil jwtUtil;

	private final CustomUserDetailsDaoService userDetailsService;

	private final BlockedTokenService blockedTokenService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String jwt = extractTokenFromCookies(request);

		if (jwt != null && !jwt.isBlank()) {
			authenticate(jwt, request, response);
		}

		// ALWAYS continue, and OUTSIDE any try/catch. Exceptions thrown later
		// (controller,
		// services) are no longer mistaken for authentication failures.
		filterChain.doFilter(request, response);
	}

	private void authenticate(String jwt, HttpServletRequest request, HttpServletResponse response) {
		try {
			String userIdStr = jwtUtil.extractUserId(jwt); // throws if expired / invalid

			if (blockedTokenService.checkIfPresent(jwt)) {
				reject(request, response, "TOKEN_REVOKED");
				return;
			}

			if (userIdStr == null || SecurityContextHolder.getContext().getAuthentication() != null) {
				return;
			}

			UserDetails userDetails = userDetailsService.loadUserByUsername(userIdStr);

			// A blocked / deactivated account must stop working immediately
			if (!userDetails.isEnabled()) {
				reject(request, response, "ACCOUNT_BLOCKED");
				return;
			}

			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null,
					userDetails.getAuthorities());
			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authToken);

			log.debug("Authenticated {} with {}", userDetails.getUsername(), userDetails.getAuthorities());

		} catch (ExpiredJwtException e) {
			reject(request, response, "TOKEN_EXPIRED");
		} catch (JwtException | IllegalArgumentException e) {
			reject(request, response, "TOKEN_INVALID");
		} catch (UsernameNotFoundException e) {
			reject(request, response, "USER_NOT_FOUND");
		}
	}

	/**
	 * Treat the request as logged-out and tell the browser to drop the dead cookie.
	 */
	private void reject(HttpServletRequest request, HttpServletResponse response, String code) {
		log.warn("Rejecting auth cookie on {} {}: {}", request.getMethod(), request.getRequestURI(), code);
		request.setAttribute(AUTH_ERROR_ATTRIBUTE, code);
		clearAuthCookie(response);
	}

	private void clearAuthCookie(HttpServletResponse response) {
		ResponseCookie cleared = ResponseCookie.from(authCookieName, "").httpOnly(true).secure(cookieSecure)
				.sameSite(cookieSameSite).path("/").maxAge(0).build();

		response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
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

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();

		return "OPTIONS".equalsIgnoreCase(request.getMethod()) // CORS preflight
				|| path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v2/user/register-init")
				|| path.startsWith("/api/v2/user/verify-otp");
	}
}