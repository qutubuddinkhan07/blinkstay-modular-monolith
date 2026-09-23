package blinkstay.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JWTUtil {
	@Value("${jwt.signature}")
	private String jwtSignature;

	private SecretKey secret_key;

	@PostConstruct
	public void assignKey() {
		secret_key = Keys.hmacShaKeyFor(jwtSignature.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 
	 * Target Duration Milliseconds Expression Total Milliseconds 15 Minutes 1000 *
	 * 60 * 15 900,000 1 Hour (Current) 1000 * 60 * 60 3,600,000 1 Day (24 Hours)
	 * 1000 * 60 * 60 * 24 86,400,000 7 Days 1000 * 60 * 60 * 24 * 7 604,800,000
	 */
	// Passing user.getId().toString() as the subject here
	public String generateToken(String userIdStr, List<String> roles) {
		String token = Jwts.builder().subject(userIdStr).claim("roles", roles).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)).signWith(secret_key).compact();

		return token;
	}

	public Claims extractClaims(String token) {
		return Jwts.parser().verifyWith(secret_key).build().parseSignedClaims(token).getPayload();
	}

	// Extracts the UUID string from the subject claim
	public String extractUserId(String token) {
		return extractClaims(token).getSubject();
	}

	// Alias for backward compatibility if needed elsewhere
	public String extractUsername(String token) {
		return extractClaims(token).getSubject();
	}

	public boolean validationToken(String token, UserDetails userDetails) {
		String userIdStr = extractUserId(token);
		return userIdStr.equals(userDetails.getUsername());
	}

	public boolean isExpired(String token) {
		return extractClaims(token).getExpiration().before(new Date());
	}

	@SuppressWarnings("unchecked")
	public List<String> extractRoles(String token) {
		return extractClaims(token).get("roles", List.class);
	}

	public Date extractExpiry(String token) {
		return extractClaims(token).getExpiration();
	}
}
