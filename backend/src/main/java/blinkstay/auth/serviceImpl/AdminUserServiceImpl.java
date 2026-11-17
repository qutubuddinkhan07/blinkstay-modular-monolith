package blinkstay.auth.serviceImpl;

import blinkstay.auth.dtos.AdminUserDto;
import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.mapper.ModelMapper;
import blinkstay.auth.repository.UserRepository;
import blinkstay.auth.repository.UserSpecifications;
import blinkstay.auth.service.AdminUserService;
import blinkstay.common.booking.BookingGuard;
import blinkstay.common.dto.PageResult;
import blinkstay.common.events.UserBlockedEvent;
import blinkstay.common.events.UserRestoredEvent;
import blinkstay.common.events.UserUnblockedEvent;
import blinkstay.common.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher publisher;
    private final BookingGuard bookingGuard;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "authUsers", allEntries = true),
            @CacheEvict(value = "usersById", key = "#targetId"),
            @CacheEvict(value = "usersByEmail", allEntries = true),
            @CacheEvict(value = "hotelManagerIds", allEntries = true)})
    public void restoreUser(UUID targetId) {
        // row lock: the nightly purge cannot run on this user at the same moment
        User user = userRepository.findByIdForUpdate(targetId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getDeletedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is not deleted");
        }

        boolean stillBlocked = user.getBlockedAt() != null;
        boolean dataPurged = user.getPurgedAt() != null;

        user.setDeletedAt(null);
        user.setPurgedAt(null); // if they delete again later, the 30-day clock starts over
        user.setIsActive(!stillBlocked);
        userRepository.save(user);

        publisher.publishEvent(new UserRestoredEvent(targetId, stillBlocked, dataPurged));
        log.info("User {} restored{}", targetId, stillBlocked ? " (still blocked)" : "");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminUserDto> searchUsers(String q, Boolean blocked, boolean deleted, UserRole role, int page,
                                                int size) {
        String term = (q == null || q.isBlank()) ? null : q.trim();
        // deleted list: most recently deleted first
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, deleted ? "deletedAt" : "createdAt"));

        Page<User> result = userRepository.findAll(UserSpecifications.adminSearch(term, blocked, deleted, role), pageable);
        return new PageResult<>(result.getContent().stream().map(modelMapper::userToAdminUserDto).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "authUsers", allEntries = true),
            @CacheEvict(value = "usersById", key = "#targetId"),
            @CacheEvict(value = "usersByEmail", allEntries = true),
            @CacheEvict(value = "hotelManagerIds", allEntries = true)})
    public void blockUser(UUID adminId, UUID targetId, String reason) {
        if (adminId.equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot block yourself");
        }
        User user = findLiveUser(targetId);

        if (user.getRoles().contains(UserRole.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot be blocked");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already blocked");
        }

        // blocking suspends all of a host's listings, so it must not strand paid guests
        if (bookingGuard.hostHasActiveBookings(targetId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This host has upcoming guest bookings on their listings. Cancel them before blocking.");
        }

        user.setIsActive(false);
        user.setBlockReason(reason.trim());
        user.setBlockedAt(LocalDateTime.now());
        userRepository.save(user);

        // runs the listing listener inside this transaction: all or nothing
        publisher.publishEvent(new UserBlockedEvent(targetId, reason.trim()));
        log.info("User {} blocked", targetId);
    }

    @Override
    @Transactional
    @Caching(evict = {@CacheEvict(value = "usersById", key = "#targetId"),
            @CacheEvict(value = "usersByEmail", allEntries = true),
            @CacheEvict(value = "hotelManagerIds", allEntries = true)})
    public void unblockUser(UUID targetId) {
        User user = findLiveUser(targetId);

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is not blocked");
        }

        user.setIsActive(true);
        user.setBlockReason(null);
        user.setBlockedAt(null);
        userRepository.save(user);

        publisher.publishEvent(new UserUnblockedEvent(targetId));
        log.info("User {} unblocked", targetId);
    }

    // self-deleted accounts count as not found, so they can never be unblocked
    private User findLiveUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User not found");
        }
        return user;
    }
}
