package blinkstay.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.auth.dto.ApiResponse;
import blinkstay.auth.dto.LoginUserDto;
import blinkstay.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<String>> authenticateUsernamePasswordController(@RequestBody LoginUserDto dto) {
		String serviceResponse = authService.authUsernameAndPasswordService(dto.getUsername(), dto.getPassword());
		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(true).message("User authentication")
				.data(serviceResponse).build();

		return ResponseEntity.ok(apiResponse);
	}

	@PostMapping("/logout")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<String>> logoutController(HttpServletRequest request) {
		String serviceResponse = authService.logoutService(request);
		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(true).message("logout service")
				.data(serviceResponse).build();

		return ResponseEntity.ok(apiResponse);
	}

}
