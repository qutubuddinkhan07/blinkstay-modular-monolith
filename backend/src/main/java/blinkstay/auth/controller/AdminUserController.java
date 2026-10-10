package blinkstay.auth.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.auth.dtos.AdminUserDto;
import blinkstay.auth.dtos.ApiResponse;
import blinkstay.auth.dtos.BlockedUserDto;
import blinkstay.auth.dtos.PageResult;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.service.AdminUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/admin/users")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {
	private final AdminUserService adminUserService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResult<AdminUserDto>>> search(@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean blocked, @RequestParam(required = false) UserRole role,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(ApiResponse.<PageResult<AdminUserDto>>builder().success(true).message("Users")
				.data(adminUserService.searchUsers(q, blocked, role, page, size)).build());
	}

	@PostMapping("/{userId}/block")
	public ResponseEntity<ApiResponse<String>> block(@AuthenticationPrincipal UserDetails ud,
			@PathVariable("userId") UUID userId, @Valid @RequestBody BlockedUserDto dto) {
		adminUserService.blockUser(UUID.fromString(ud.getUsername()), userId, dto.getReason());
		return ResponseEntity
				.ok(ApiResponse.<String>builder().success(true).message("User blocked").data(null).build());
	}

	@PostMapping("/{userId}/unblock")
	public ResponseEntity<ApiResponse<String>> unblock(@PathVariable("userId") UUID userId) {
		adminUserService.unblockUser(userId);
		return ResponseEntity
				.ok(ApiResponse.<String>builder().success(true).message("User unblocked").data(null).build());
	}
}
