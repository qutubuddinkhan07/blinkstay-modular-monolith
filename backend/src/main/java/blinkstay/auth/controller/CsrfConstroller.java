package blinkstay.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class CsrfConstroller {

	@GetMapping("/csrf")
	public ResponseEntity<?> csrf(CsrfToken csrfToken) {
		return ResponseEntity.ok(csrfToken);
	}
}
