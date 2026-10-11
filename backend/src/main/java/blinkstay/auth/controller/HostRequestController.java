package blinkstay.auth.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.auth.dtos.ApiResponse;
import blinkstay.auth.dtos.HostRequestDto;
import blinkstay.auth.dtos.HostRequestResponseDto;
import blinkstay.auth.service.HostRequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/host-requests")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class HostRequestController {
	private final HostRequestService hostRequestService;

	@PostMapping
	public ResponseEntity<ApiResponse<HostRequestResponseDto>> apply(@AuthenticationPrincipal UserDetails userDetails,
			@Valid @RequestBody HostRequestDto dto) {
		UUID userId = UUID.fromString(userDetails.getUsername());
		HostRequestResponseDto res = hostRequestService.apply(userId, dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(true, "Request submitted", res));
	}

	// data is null when the user has never applied
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<HostRequestResponseDto>> myRequest(
			@AuthenticationPrincipal UserDetails userDetails) {
		HostRequestResponseDto res = hostRequestService.getMyLatesetRequest(UUID.fromString(userDetails.getUsername()));
		return ResponseEntity
				.ok(ApiResponse.of(true, res == null ? "No host request yet" : "Latest host request", res));
	}

	@DeleteMapping("/me")
	public ResponseEntity<ApiResponse<String>> cancel(@AuthenticationPrincipal UserDetails userDetails) {
		hostRequestService.cancelMyRequeset(UUID.fromString(userDetails.getUsername()));
		return ResponseEntity.ok(ApiResponse.of(true, "Request cancelled", null));
	}
}
