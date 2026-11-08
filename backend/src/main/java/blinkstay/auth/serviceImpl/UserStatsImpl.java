package blinkstay.auth.serviceImpl;

import blinkstay.auth.enums.UserRole;
import blinkstay.auth.repository.UserRepository;
import blinkstay.common.stats.UserCounts;
import blinkstay.common.stats.UserStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserStatsImpl implements UserStats {
    private final UserRepository userRepo;

    @Override
    @Transactional(readOnly = true)
    public UserCounts counts(LocalDate from, LocalDate to) {
        return new UserCounts(userRepo.countActiveAccounts(), userRepo.countActiveByRole(UserRole.HOTEL_MANAGER),
                userRepo.countActiveByRole(UserRole.ADMIN), userRepo.countBlocked(),
                userRepo.countCreatedBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
    }
}