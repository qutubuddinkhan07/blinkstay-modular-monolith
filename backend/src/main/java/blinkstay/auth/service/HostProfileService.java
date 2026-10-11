package blinkstay.auth.service;

import java.util.UUID;

import blinkstay.auth.dtos.HostProfileDto;
import blinkstay.auth.dtos.UpdateHostProfileDto;

public interface HostProfileService {
	HostProfileDto getMyProfile(UUID userId);

	HostProfileDto updateMyProfile(UUID userId, UpdateHostProfileDto dto);
}
