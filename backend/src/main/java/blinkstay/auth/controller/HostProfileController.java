package blinkstay.auth.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.auth.dtos.ApiResponse;
import blinkstay.auth.dtos.HostProfileDto;
import blinkstay.auth.dtos.UpdateHostProfileDto;
import blinkstay.auth.service.HostProfileService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/hosts")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Berer Authentication")
@PreAuthorize("hasAuthority('HOTEL_MANAGER')")
public class HostProfileController {
	private final HostProfileService hostProfileService;

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<HostProfileDto>> myProfile(@AuthenticationPrincipal UserDetails userDetails) {
		HostProfileDto res = hostProfileService.getMyProfile(UUID.fromString(userDetails.getUsername()));
		return ResponseEntity.ok(ApiResponse.of(true, res == null ? " No host profile yet" : "Host profile", res));
	}

	@PatchMapping("/me")
	public ResponseEntity<ApiResponse<HostProfileDto>> updateMyProfile(@AuthenticationPrincipal UserDetails userDetails,
			@Valid @RequestBody UpdateHostProfileDto dto) {
		HostProfileDto res = hostProfileService.updateMyProfile(UUID.fromString(userDetails.getUsername()), dto);
		return ResponseEntity.ok(ApiResponse.of(true, "Host profile updated", res));
	}
}
