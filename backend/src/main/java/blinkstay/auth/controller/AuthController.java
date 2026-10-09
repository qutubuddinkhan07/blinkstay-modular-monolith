package blinkstay.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.auth.dtos.ApiResponse;
import blinkstay.auth.dtos.LoginUserDto;
import blinkstay.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<String>> authenticateUsernamePasswordController(@RequestBody LoginUserDto dto,
			HttpServletResponse response) {
		authService.authUsernameAndPasswordService(dto.getUsername(), dto.getPassword(), response);

		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(true).message("Login Successful!")
				.data(null).build();

		return ResponseEntity.ok(apiResponse);
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<String>> logoutController(HttpServletRequest request,
			HttpServletResponse response) {
		String serviceResponse = authService.logoutService(request, response);
		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(true).message("Logout Successful!")
				.data(serviceResponse).build();

		return ResponseEntity.ok(apiResponse);
	}

}
