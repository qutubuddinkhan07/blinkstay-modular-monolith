package com.blinkstay.auth.serviceImpl;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blinkstay.auth.dtos.HostProfileDto;
import com.blinkstay.auth.dtos.UpdateHostProfileDto;
import com.blinkstay.auth.entities.HotelManagerProfile;
import com.blinkstay.auth.mapper.ModelMapper;
import com.blinkstay.auth.repository.HotelManagerProfileRepository;
import com.blinkstay.auth.service.HostProfileService;
import com.blinkstay.common.events.HostDetailsChangedEvent;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HostProfileServiceImpl implements HostProfileService {
	private final HotelManagerProfileRepository profileRepo;
	private final ModelMapper modelMapper;
	private final ApplicationEventPublisher publisher;

	@Override
	@Transactional(readOnly = true)
	public HostProfileDto getMyProfile(UUID userId) {
		return profileRepo.findByUserId(userId).map(modelMapper::hostProfileToDto).orElse(null);
	}

	@Override
	@Transactional
	public HostProfileDto updateMyProfile(UUID userId, UpdateHostProfileDto dto) {
		// hosts added by hand have no profile row yet, so the first save creates it
		HotelManagerProfile profile = profileRepo.findByUserId(userId)
				.orElseGet(() -> HotelManagerProfile.builder().userId(userId).build());

		if (dto.getBusinessName() != null && !dto.getBusinessName().isBlank()) {
			profile.setBusinessName(dto.getBusinessName().trim());
		}

		if (dto.getAddress() != null && !dto.getAddress().isBlank()) {
			profile.setAddress(dto.getAddress());
		}

		if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
			profile.setDescription(dto.getDescription());
		}

		if (dto.getWebsite() != null) {
			profile.setWebsite(dto.getWebsite());
		}

		HotelManagerProfile saved = profileRepo.save(profile);
		publisher.publishEvent(new HostDetailsChangedEvent(userId));

		return modelMapper.hostProfileToDto(saved);
	}

}
