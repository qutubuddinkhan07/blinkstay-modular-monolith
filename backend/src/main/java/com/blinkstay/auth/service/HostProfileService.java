package com.blinkstay.auth.service;

import java.util.UUID;

import com.blinkstay.auth.dtos.HostProfileDto;
import com.blinkstay.auth.dtos.UpdateHostProfileDto;

public interface HostProfileService {
	HostProfileDto getMyProfile(UUID userId);

	HostProfileDto updateMyProfile(UUID userId, UpdateHostProfileDto dto);
}
