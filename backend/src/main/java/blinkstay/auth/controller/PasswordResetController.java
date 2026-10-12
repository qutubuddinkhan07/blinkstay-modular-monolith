package blinkstay.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.auth.dtos.ApiResponse;
import blinkstay.auth.dtos.ForgotPasswordDto;
import blinkstay.auth.dtos.ResetPasswordDto;
import blinkstay.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class PasswordResetController {
	private final PasswordResetService passwordResetService;

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordDto dto) {
		passwordResetService.requestReset(dto.getUsername(), dto.getEmail());
		return ResponseEntity
				.ok(ApiResponse.of(true, "If the details match an account, a verification code has been sent.", null));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
		passwordResetService.resetPassword(dto.getEmail(), dto.getOtp(), dto.getNewPassword());
		return ResponseEntity.ok(ApiResponse.<String>builder().success(true)
				.message("Password updated. You can log in with your new password.").data(null).build());
	}
}
