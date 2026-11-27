package com.blinkstay.auth.serviceImpl;

import com.blinkstay.auth.entities.BlockedToken;
import com.blinkstay.auth.repository.UserRepository;
import com.blinkstay.auth.scheduler.DeletedAccountPurgeJob;
import com.blinkstay.auth.service.AuthService;
import com.blinkstay.auth.service.BlockedTokenService;
import com.blinkstay.auth.util.JWTUtil;
import com.blinkstay.common.exception.AccountBlockedException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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

    private final BlockedTokenService blockedTokenService;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter RESTORE_DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.CANADA.ENGLISH);

    @Override
    public void authUsernameAndPasswordService(String username, String password, HttpServletResponse response) {
        // emails are stored lowercase, so "Naruto@Gmail.com" must still log in
        String email = username == null ? "" : username.trim().toLowerCase();

        try {
            Authentication authentication = authManager
                    .authenticate(new UsernamePasswordAuthenticationToken(email, password));

            // the principal's username is the user id (see CustomUserDetailsDaoService)
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userIdStr = userDetails.getUsername();

            List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .filter(auth -> !auth.startsWith("FACTOR_")).toList();

            log.info("Login successful for userId {} with roles {}", userIdStr, roles);

            writeAuthCookie(jwtUtil.generateToken(userIdStr, roles), response);
        } catch (DisabledException e) {
            // Spring checks "disabled" BEFORE the password, so only someone who knows the
            // password learns why the account cannot log in
            userRepository.findByEmail(email).filter(u -> passwordEncoder.matches(password, u.getPassword()))
                    .ifPresent(u -> {
                        if (u.getDeletedAt() != null) {
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, deletedAccountMessage(u.getDeletedAt()));
                        }
                        throw new AccountBlockedException(u.getBlockReason());
                    });
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for {}: {} - {}", email, e.getClass().getSimpleName(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    private String deletedAccountMessage(LocalDateTime deletedAt) {
        LocalDateTime lastDay = deletedAt.plusDays(DeletedAccountPurgeJob.RETENTION_DAYS);
        if (LocalDateTime.now().isBefore(lastDay)) {
            return "This account was deleted. Contact support before " + RESTORE_DAY.format(lastDay)
                    + " to restore it with all your details intact.";
        }
        return "This account was deleted. Contact support if you want it restored.";
    }

    @Override
    public String logoutService(HttpServletRequest request, HttpServletResponse response) {
        String jwt = extractTokenFromCookies(request);

        if (jwt == null) {
            clearAuthCookie(response);
            return "No JWT found";
        }

        LocalDateTime expireAt;
        try {
            expireAt = jwtUtil.extractExpiry(jwt).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (JwtException | IllegalArgumentException e) {
            // expired or invalid: nothing left to revoke, just clear the cookie
            clearAuthCookie(response);
            return "Session already expired";
        }

        if (blockedTokenService.checkIfPresent(jwt)) {
            clearAuthCookie(response);
            return "Already logged out";
        }

        BlockedToken token = BlockedToken.builder().token(jwt).blockedAt(LocalDateTime.now()).expiresAt(expireAt)
                .build();
        blockedTokenService.blockToken(jwt, token);

        clearAuthCookie(response);
        return "Logout successful";
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

    private void writeAuthCookie(String jwt, HttpServletResponse response) {
        ResponseCookie authCookie = ResponseCookie.from(authCookieName, jwt).httpOnly(true).secure(cookieSecure)
                .sameSite(cookieSameSite).path("/").maxAge(Duration.ofSeconds(cookieMaxAge)).build();
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
    }

    @Override
    public void reissueSession(UserDetails userDetails, HttpServletResponse response) {
        List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        writeAuthCookie(jwtUtil.generateToken(userDetails.getUsername(), roles), response);
    }

}
