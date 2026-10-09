package blinkstay.auth.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import blinkstay.auth.filter.JWTFilter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SpringSecurityConfiguration {
	@Autowired
	private JWTFilter jwtFilter;

	@Bean
	public SecurityFilterChain configureSecurityFilterChain(HttpSecurity http) throws Exception {

		http
				// CSRF PROTECTION
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/auth/login", "/api/v1/auth/logout",
						"/api/v1/auth/csrf", "/api/v2/user/register-init", "/api/v2/user/verify-otp").spa())

				// CORS
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				// JWT authentication remains STATELESS
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// no sessions, JWT only
				.authorizeHttpRequests(auth -> auth
						// Preflight requests
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

						// Public requests
//						.requestMatchers("/api/v1/auth/login", "/api/v1/auth/csrf", "/api/v2/user/register-init",
//								"/api/v2/user/verify-otp", "/api/v3/listings/:id", "/swagger-ui/**", "/swagger-ui.html",
//								"/v3/api-docs/**")
//						.permitAll() ============

						.requestMatchers("/api/v1/auth/login", "/api/v1/auth/csrf", "/api/v2/user/register-init",
								"/api/v2/user/verify-otp", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
						.permitAll()

						.requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()

						.requestMatchers(HttpMethod.GET, "/api/v3/listings/all").permitAll()

						// Must come BEFORE the {listingId} rule, otherwise "my-listings"
						// would be treated as a listing id
						.requestMatchers(HttpMethod.GET, "/api/v3/listings/my-listings").hasAuthority("HOTEL_MANAGER")

						.requestMatchers(HttpMethod.GET, "/api/v3/listings/{listingId}").permitAll()

						// Listings authorization
						.requestMatchers("/api/v3/listings/**").hasAnyAuthority("HOTEL_MANAGER", "ADMIN")

						// User APIs
						.requestMatchers("/api/v2/user/**").hasAnyAuthority("USER", "HOTEL_MANAGER", "ADMIN")

						// Everything else requires authentication
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						// This handles unauthorized requests - return 401 not 500
						.authenticationEntryPoint((request, response, authException) -> {
							Object reason = request.getAttribute(JWTFilter.AUTH_ERROR_ATTRIBUTE);
							String code = reason != null ? reason.toString() : "UNAUTHENTICATED";

							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType("application/json");
							response.getWriter()
									.write("{\"message\": \"Authentication required\", \"code\": \"" + code + "\"}");
						}))
				.formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())

				// REGISTER FILTER HERE
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://localhost:5173",
				// VS Code Dev Tunnels
				"https://*.devtunnels.ms",

				// ngrok
				"https://*.ngrok-free.app", "https://*.ngrok.io",

				// Netlify
				"https://*.netlify.app"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		config.setAllowedHeaders(List.of("*"));

		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public PasswordEncoder createPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager createAuthManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	/**
	 * JWTFilter is a @Component, so Spring Boot would also register it as a plain
	 * servlet filter. It already runs inside the security chain (addFilterBefore),
	 * so switch the second registration off.
	 */
	@Bean
	public FilterRegistrationBean<JWTFilter> jwtFilterRegistration(JWTFilter filter) {
		FilterRegistrationBean<JWTFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}
}
