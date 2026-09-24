package blinkstay.auth.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
		http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// no sessions, JWT only
				.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/v1/auth/login", "/api/v2/user/register-init", "/api/v2/user/verify-otp",
								"/api/v3/listings/all", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
						.permitAll()

						// Hotel manager to create listings
						.requestMatchers("/api/v3/listings/**").hasAnyAuthority("HOTEL_MANAGER", "ADMIN")
						.requestMatchers("/api/v2/user/**").hasAnyAuthority("USER", "HOTEL_MANAGER", "ADMIN")
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						// This handles unauthorized requests - return 401 not 500
						.authenticationEntryPoint((request, response, authException) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType("application/json");
							response.getWriter().write("{\"message\": \"Invalid credentials\"}");
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
				"https://*.ngrok-free.app", "https://*.ngrok.io", "https://*.netlify.app"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("Authorization"));
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
}
