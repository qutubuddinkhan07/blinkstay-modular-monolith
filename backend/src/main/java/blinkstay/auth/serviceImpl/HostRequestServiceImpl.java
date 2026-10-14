package blinkstay.auth.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.auth.dtos.AdminHostRequestDto;
import blinkstay.auth.dtos.HostRequestDto;
import blinkstay.auth.dtos.HostRequestResponseDto;
import blinkstay.auth.entities.HotelManagerProfile;
import blinkstay.auth.entities.HotelManagerRequest;
import blinkstay.auth.entities.User;
import blinkstay.auth.enums.RequestStatus;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.mapper.ModelMapper;
import blinkstay.auth.repository.HotelManagerProfileRepository;
import blinkstay.auth.repository.HotelManagerRequestRepository;
import blinkstay.auth.repository.UserRepository;
import blinkstay.auth.service.HostRequestService;
import blinkstay.common.cache.AfterCommitCacheEvictor;
import blinkstay.common.dto.PageResult;
import blinkstay.common.events.HotelManagerRequestReviewedEvent;
import blinkstay.common.events.HotelManagerRequestSubmittedEvent;
import blinkstay.common.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostRequestServiceImpl implements HostRequestService {
	private final HotelManagerRequestRepository requestRepo;
	private final HotelManagerProfileRepository profileRepo;
	private final UserRepository userRepo;
	private final ModelMapper modelMapper;
	private final ApplicationEventPublisher publisher;
	private final AfterCommitCacheEvictor cacheEvictor;

	@Override
	@Transactional
	public HostRequestResponseDto apply(UUID userId, HostRequestDto dto) {
		User user = userRepo.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));

		if (user.getRoles().contains(UserRole.HOTEL_MANAGER)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a host");
		}

		if (requestRepo.findByUserIdAndStatus(userId, RequestStatus.PENDING).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have a pending request");
		}

		HotelManagerRequest request = HotelManagerRequest.builder().userId(userId).status(RequestStatus.PENDING)
				.pendingUserId(userId).businessName(dto.getBusinessName().trim()).address(dto.getAddress().trim())
				.description(dto.getDescription().trim()).website(blankToNull(dto.getWebsite())).build();

		try {
			request = requestRepo.saveAndFlush(request);
		} catch (DataIntegrityViolationException e) {
			// two requests at the same moment: the unique contraint let only one through
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have a pending request");
		}

		publisher.publishEvent(new HotelManagerRequestSubmittedEvent(request.getId(), userId, user.getUsername()));
		return modelMapper.hostRequestToDto(request);
	}

	@Override
	@Transactional(readOnly = true)
	public HostRequestResponseDto getMyLatesetRequest(UUID userId) {
		return requestRepo.findFirstByUserIdOrderByCreatedAtDesc(userId).map(modelMapper::hostRequestToDto)
				.orElse(null);
	}

	@Override
	public void cancelMyRequeset(UUID userId) {
		HotelManagerRequest request = requestRepo.findByUserIdAndStatus(userId, RequestStatus.PENDING)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You have no pending request"));

		request.setStatus(RequestStatus.CANCELLED);
		request.setPendingUserId(null);
		requestRepo.save(request);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<AdminHostRequestDto> listRequests(RequestStatus status, int page, int size) {
		// oldest first for the pending queue, newest first for everything else
		Sort sort = Sort.by(status == RequestStatus.PENDING ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), sort);

		Page<HotelManagerRequest> result = status == null ? requestRepo.findAll(pageable)
				: requestRepo.findByStatus(status, pageable);

		List<UUID> userId = result.getContent().stream().map(HotelManagerRequest::getUserId).distinct().toList();
		Map<UUID, User> users = userRepo.findAllById(userId).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));

		List<AdminHostRequestDto> content = result.getContent().stream()
				.map(r -> modelMapper.hostRequestToAdminDto(r, users.get(r.getId()))).toList();

		return new PageResult<>(content, result.getNumber(), result.getSize(), result.getTotalElements(),
				result.getTotalPages());
	}

	@Override
	public long countPending() {
		return requestRepo.countByStatus(RequestStatus.PENDING);
	}

	@Override
	public void approve(UUID adminId, UUID requestId) {
		HotelManagerRequest request = loadReviewable(requestId, adminId);

		User user = userRepo.findById(request.getUserId()).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.CONFLICT, "The application no longer exists"));

		if (user.getDeletedAt() != null || !Boolean.TRUE.equals(user.getIsActive())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The applicant account is not active");
		}

		LocalDateTime now = LocalDateTime.now();

		user.getRoles().add(UserRole.HOTEL_MANAGER);
		userRepo.save(user);

		HotelManagerProfile profile = profileRepo.findByUserId(user.getId())
				.orElseGet(() -> HotelManagerProfile.builder().userId(user.getId()).build());
		profile.setBusinessName(request.getBusinessName());
		profile.setAddress(request.getAddress());
		profile.setDescription(request.getDescription());
		profile.setWebsite(request.getWebsite());
		profile.setApprovedAt(now);
		profileRepo.save(profile);

		request.setStatus(RequestStatus.APPROVED);
		request.setReviewedBy(adminId);
		request.setReviewedAt(now);
		request.setPendingUserId(null);
		requestRepo.save(request);

		publisher.publishEvent(new HotelManagerRequestReviewedEvent(request.getId(), user.getId(), true, null));
		cacheEvictor.clear("usersById", "usersByEmail", "hotelManagerIds");
		log.info("Host request {} approved by {}", requestId, adminId);
	}

	@Override
	@Transactional
	public void reject(UUID adminId, UUID requestId, String reason) {
		HotelManagerRequest request = loadReviewable(requestId, adminId);

		request.setStatus(RequestStatus.REJECTED);
		request.setRejectionReason(reason);
		request.setReviewedBy(adminId);
		request.setReviewedAt(LocalDateTime.now());
		request.setPendingUserId(null);
		requestRepo.save(request);

		publisher.publishEvent(
				new HotelManagerRequestReviewedEvent(request.getId(), request.getUserId(), false, reason.trim()));
		log.info("Host reqeust {} rejected by {}", requestId, adminId);
	}

	private HotelManagerRequest loadReviewable(UUID requestId, UUID adminId) {
		HotelManagerRequest request = requestRepo.findById(requestId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

		if (request.getStatus() != RequestStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This request is already " + request.getStatus().name().toLowerCase());
		}

		if (request.getUserId().equals(adminId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot review your own request");
		}

		return request;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
