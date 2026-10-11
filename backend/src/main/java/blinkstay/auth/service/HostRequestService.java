package blinkstay.auth.service;

import java.util.UUID;

import blinkstay.auth.dtos.AdminHostRequestDto;
import blinkstay.auth.dtos.HostRequestDto;
import blinkstay.auth.dtos.HostRequestResponseDto;
import blinkstay.auth.dtos.PageResult;
import blinkstay.auth.enums.RequestStatus;

public interface HostRequestService {
	HostRequestResponseDto apply(UUID userId, HostRequestDto dto);

	HostRequestResponseDto getMyLatesetRequest(UUID userId);

	void cancelMyRequeset(UUID userId);

	PageResult<AdminHostRequestDto> listRequests(RequestStatus status, int page, int size);

	long countPending();

	void approve(UUID adminId, UUID requestId);

	void reject(UUID adminId, UUID requestId, String reason);
}
