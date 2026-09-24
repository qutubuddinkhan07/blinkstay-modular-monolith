package blinkstay.auth.filter;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
	private final JWTUtil jwtUtil;

	private final CustomUserDetailsDaoService userDetailsService;

	private final BlockedTokenService blockedTokenService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		System.out.println("JWT FILTER");
		System.out.println("Request: " + request.getRequestURI());
		System.out.println("Authorization: " + request.getHeader("Authorization"));

		// Skip filter if no Bearer token (public endpoint)
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String jwt = authHeader.substring(7);

		try {
			// 1. Extract UUID string from JWT subject
			String userIdStr = jwtUtil.extractUserId(jwt);

			if (blockedTokenService.checkIfPresent(jwt)) {
				writeError(response, "Token has been revoked/logged out");
				return;
			}

			// 2. Fetch UserDetails by UUID string
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

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String path = request.getRequestURI();
		return path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v2/user/register-init")
				|| path.startsWith("/api/v2/user/verify-otp") || path.startsWith("/api/v3/listings/all");
	}

	private void writeError(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		response.getWriter().write("{\"message\": \"" + message + "\"}");
	}
}
