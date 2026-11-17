package blinkstay.auth.controller;

import blinkstay.auth.dtos.AdminUserDto;
import blinkstay.auth.dtos.ApiResponse;
import blinkstay.auth.dtos.BlockedUserDto;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.service.AdminUserService;
import blinkstay.common.dto.PageResult;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/admin/users")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {
    private final AdminUserService adminUserService;

    // ?deleted=true lists only deleted accounts (default: live accounts)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<AdminUserDto>>> search(@RequestParam(required = false) String q,
                                                                        @RequestParam(required = false) Boolean blocked,
                                                                        @RequestParam(defaultValue = "false") boolean deleted,
                                                                        @RequestParam(required = false) UserRole role, @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResult<AdminUserDto>>builder().success(true).message("Users")
                .data(adminUserService.searchUsers(q, blocked, deleted, role, page, size)).build());
    }

    @PostMapping("/{userId}/restore")
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable("userId") UUID userId) {
        adminUserService.restoreUser(userId);
        return ResponseEntity
                .ok(ApiResponse.<String>builder().success(true).message("User restored").data(null).build());
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
