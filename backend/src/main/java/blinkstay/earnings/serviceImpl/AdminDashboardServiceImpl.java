package blinkstay.earnings.serviceImpl;

import blinkstay.common.directory.UserContact;
import blinkstay.common.directory.UserDirectory;
import blinkstay.common.stats.*;
import blinkstay.common.time.HotelClock;
import blinkstay.earnings.dto.*;
import blinkstay.earnings.enums.EarningsStatus;
import blinkstay.earnings.repository.EarningsRepository;
import blinkstay.earnings.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final EarningsRepository repo;
    private final HotelClock clock;
    private final BookingStats bookingStats;
    private final PaymentStats paymentStats;
    private final UserStats userStats;
    private final ListingStats listingStats;
    private final UserDirectory userDirectory;

    @Value("${app.earnings.currency:INR}")
    private String currency;

    private record Range(LocalDate from, LocalDate to) {
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOverviewDto overview(LocalDate from, LocalDate to) {
        Range r = range(from, to);

        long paid = 0, fullyRefunded = 0;
        BigDecimal gross = BigDecimal.ZERO, refunded = BigDecimal.ZERO, commission = BigDecimal.ZERO;
        BigDecimal owed = BigDecimal.ZERO, earned = BigDecimal.ZERO, pending = BigDecimal.ZERO;

        for (StatusTotals t : repo.platformTotals(r.from(), r.to(), currency)) {
            if (t.status() == EarningsStatus.REVERSED) {
                fullyRefunded += t.bookings();
                continue;
            }
            paid += t.bookings();
            gross = gross.add(t.gross());
            refunded = refunded.add(t.commission());
            owed = owed.add(t.hostAmount());
            if (t.status() == EarningsStatus.EARNED)
                earned = earned.add(t.hostAmount());
            if (t.status() == EarningsStatus.PENDING)
                pending = pending.add(t.hostAmount());
        }

        BigDecimal average = paid == 0 ? BigDecimal.ZERO.setScale(2)
                : gross.divide(BigDecimal.valueOf(paid), 2, RoundingMode.HALF_UP);

        long everPaid = paid + fullyRefunded;
        BigDecimal rate = everPaid == 0 ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(fullyRefunded).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(everPaid), 1, RoundingMode.HALF_UP);

        UserCounts u = userStats.counts(r.from(), r.to());

        return new AdminOverviewDto(r.from(), r.to(), currency,
                new AdminOverviewDto.Money(gross, refunded, commission, owed, earned, pending, average, paid,
                        fullyRefunded, rate),
                bookingStats.countByStatus(r.from(), r.to()),
                new AdminOverviewDto.Users(u.accounts(), u.hosts(), u.admins(), u.blocked(), u.newAccounts()),
                listingStats.countByStatus(), paymentStats.needingAttention());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformMonthlyDto> monthly(LocalDate from, LocalDate to) {
        Range r = range(from, to);

        Map<YearMonth, PlatformMonthlyDto> found = new HashMap<>();
        for (PlatformMonthlyDto m : repo.platformMonthly(r.from(), r.to(), currency)) {
            found.put(YearMonth.of(m.year(), m.month()), m);
        }

        // every month in the range appears, empty months as zeros
        List<PlatformMonthlyDto> series = new ArrayList<>();
        for (YearMonth ym = YearMonth.from(r.from());
             !ym.isAfter(YearMonth.from(r.to())); ym = ym.plusMonths(1)) {
            series.add(found.getOrDefault(ym,
                    new PlatformMonthlyDto(ym.getYear(), ym.getMonthValue(), 0, BigDecimal.ZERO, BigDecimal.ZERO)));
        }

        return series;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopHostDto> topHosts(LocalDate from, LocalDate to, int limit) {
        Range r = range(from, to);
        List<TopHostDto> rows = repo.topHosts(r.from(), r.to(), currency, PageRequest.of(0, cap(limit)));

        return rows.stream().map(h -> new TopHostDto(h.hostId(),
                userDirectory.findContact(h.hostId()).map(UserContact::username).orElse("Delete user"), h.bookings(),
                h.gross(), h.commission(), h.hostAmount())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingEarningsDto> topListings(LocalDate from, LocalDate to, int limit) {
        Range r = range(from, to);
        return repo.topListings(r.from(), r.to(), currency, PageRequest.of(0, cap(limit)));
    }

    private static int cap(int limit) {
        return Math.min(Math.max(limit, 1), 20);
    }

    private Range range(LocalDate from, LocalDate to) {
        LocalDate today = clock.today();
        LocalDate f = from != null ? from : today.minusMonths(11).withDayOfMonth(1);
        LocalDate t = to != null ? to : today;

        if (t.isBefore(f)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'to' must not be before 'from'");
        }

        if (ChronoUnit.DAYS.between(f, t) > 800) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The date range can be at most about 2 years");
        }

        return new Range(f, t);
    }
}
