package com.blinkstay.auth.service;

import java.util.UUID;

import com.blinkstay.auth.dtos.AdminHostRequestDto;
import com.blinkstay.auth.dtos.HostRequestDto;
import com.blinkstay.auth.dtos.HostRequestResponseDto;
import com.blinkstay.auth.enums.RequestStatus;
import com.blinkstay.common.dto.PageResult;

public interface HostRequestService {
	HostRequestResponseDto apply(UUID userId, HostRequestDto dto);

	HostRequestResponseDto getMyLatesetRequest(UUID userId);

	void cancelMyRequeset(UUID userId);

	PageResult<AdminHostRequestDto> listRequests(RequestStatus status, int page, int size);

	long countPending();

	void approve(UUID adminId, UUID requestId);

	void reject(UUID adminId, UUID requestId, String reason);
}
