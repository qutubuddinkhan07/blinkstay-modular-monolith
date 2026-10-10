package blinkstay.auth.service;

import java.util.UUID;

import blinkstay.auth.dtos.AdminUserDto;
import blinkstay.auth.dtos.PageResult;
import blinkstay.auth.enums.UserRole;

public interface AdminUserService {
	PageResult<AdminUserDto> searchUsers(String q, Boolean blocked, UserRole role, int page, int size);

	void blockUser(UUID adminId, UUID targetId, String reason);

	void unblockUser(UUID targetId);
}
