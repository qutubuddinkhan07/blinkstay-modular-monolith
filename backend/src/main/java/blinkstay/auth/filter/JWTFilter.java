package blinkstay.auth.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
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

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
	@Value("${app.cookie.name}")
	private String authCookieName;

	private final JWTUtil jwtUtil;

	private final CustomUserDetailsDaoService userDetailsService;

	private final BlockedTokenService blockedTokenService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		System.out.println("JWT FILTER");
		System.out.println("Request: " + request.getRequestURI());

		String jwt = extractTokenFromCookies(request);

		/*
		 * No authentication cookie.
		 *
		 * We DO NOT immediately return 401 here.
		 *
		 * Public endpoints must continue through the filter chain. Protected endpoints
		 * will eventually be rejected by Spring Security.
		 */

		if (jwt == null || jwt.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			// 1. Extract UUID string from JWT subject
			String userIdStr = jwtUtil.extractUserId(jwt);

			// 2. Check whether token has been revoked

			if (blockedTokenService.checkIfPresent(jwt)) {
				writeError(response, "Token has been revoked/logged out");
				return;
			}

			// 3. Build Spring Security authentication
			if (userIdStr != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(userIdStr);

				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}

			filterChain.doFilter(request, response);
		} catch (ExpiredJwtException e) {
			writeError(response, "JWT expired");
		} catch (JwtException e) {
			writeError(response, "JWT Invalid");
		} catch (UsernameNotFoundException e) {
			writeError(response, "User not found");
		}
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
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String path = request.getRequestURI();
		return path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v2/user/register-init")
				|| path.startsWith("/api/v2/user/verify-otp") || path.startsWith("/api/v3/listings/all")
				|| path.startsWith("/api/v1/auth/csrf");
	}

	private void writeError(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		response.getWriter().write("{\"message\": \"" + message + "\"}");
	}
}
