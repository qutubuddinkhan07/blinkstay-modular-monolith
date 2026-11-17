package blinkstay.auth.service;

import blinkstay.auth.dtos.AdminUserDto;
import blinkstay.auth.enums.UserRole;
import blinkstay.common.dto.PageResult;

import java.util.UUID;

public interface AdminUserService {
    PageResult<AdminUserDto> searchUsers(String q, Boolean blocked, boolean deleted, UserRole role, int page, int size);

    void blockUser(UUID adminId, UUID targetId, String reason);

    void unblockUser(UUID targetId);

    /**
     * Undoes a self-delete. A user who was blocked before deleting stays blocked.
     */
    void restoreUser(UUID targetId);
}