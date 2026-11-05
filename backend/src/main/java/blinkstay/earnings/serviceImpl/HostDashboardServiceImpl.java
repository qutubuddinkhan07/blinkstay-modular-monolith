package blinkstay.earnings.serviceImpl;

import blinkstay.common.dto.PageResult;
import blinkstay.common.time.HotelClock;
import blinkstay.earnings.dto.*;
import blinkstay.earnings.entities.EarningsEntry;
import blinkstay.earnings.enums.EarningsStatus;
import blinkstay.earnings.repository.EarningsRepository;
import blinkstay.earnings.service.HostDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HostDashboardServiceImpl implements HostDashboardService {
    private final EarningsRepository repo;
    private final HotelClock clock;

    @Value("${app.earnings.currency:INR}")
    private String currency;

    private record Range(LocalDate from, LocalDate to) {
    }

    @Override
    @Transactional(readOnly = true)
    public EarningsSummaryDto summary(UUID hostId, LocalDate from, LocalDate to) {
        Range r = range(from, to);

        long bookings = 0, cancelled = 0;
        BigDecimal gross = BigDecimal.ZERO, refunded = BigDecimal.ZERO, commission = BigDecimal.ZERO;
        BigDecimal hostAmount = BigDecimal.ZERO, earned = BigDecimal.ZERO, pending = BigDecimal.ZERO;

        for (StatusTotals t : repo.totalsByStatus(hostId, r.from(), r.to(), currency)) {
            if (t.status() == EarningsStatus.REVERSED) {
                cancelled += t.bookings();
                continue;
            }
            bookings += t.bookings();
            gross = gross.add(t.gross());
            refunded = refunded.add(t.refunded());
            commission = commission.add(t.commission());
            hostAmount = hostAmount.add(t.hostAmount());
            if (t.status() == EarningsStatus.EARNED) earned = earned.add(t.hostAmount());
            if (t.status() == EarningsStatus.PENDING) pending = pending.add(t.hostAmount());
        }

        BigDecimal average = bookings == 0 ? BigDecimal.ZERO.setScale(2)
                : gross.divide(BigDecimal.valueOf(bookings), 2, RoundingMode.HALF_UP);

        return new EarningsSummaryDto(r.from(), r.to(), currency, bookings, cancelled, gross, refunded, commission,
                hostAmount, earned, pending, average);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingEarningsDto> byListing(UUID hostId, LocalDate from, LocalDate to) {
        Range r = range(from, to);
        return repo.byListing(hostId, r.from(), r.to(), currency);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyEarningsDto> monthly(UUID hostId, LocalDate from, LocalDate to) {
        Range r = range(from, to);

        Map<YearMonth, MonthlyEarningsDto> found = new HashMap<>();
        for (MonthlyEarningsDto m : repo.byMonth(hostId, r.from(), r.to(), currency)) {
            found.put(YearMonth.of(m.year(), m.month()), m);
        }

        // every month in the range appears, empty months as zeros, so a chart has no gaps
        List<MonthlyEarningsDto> series = new ArrayList<>();
        for (YearMonth ym = YearMonth.from(r.from()); !ym.isAfter(YearMonth.from(r.to())); ym = ym.plusMonths(1)) {
            series.add(found.getOrDefault(ym,
                    new MonthlyEarningsDto(ym.getYear(), ym.getMonthValue(), 0, BigDecimal.ZERO, BigDecimal.ZERO)));
        }
        return series;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<LedgerEntryDto> ledger(UUID hostId, EarningsStatus status, LocalDate from, LocalDate to,
                                             int page, int size) {
        Range r = range(from, to);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "checkIn").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        Page<EarningsEntry> result = status == null
                ? repo.findByHostIdAndCheckInBetween(hostId, r.from(), r.to(), pageable)
                : repo.findByHostIdAndStatusAndCheckInBetween(hostId, status, r.from(), r.to(), pageable);

        List<LedgerEntryDto> rows = result.getContent().stream().map(LedgerEntryDto::from).toList();
        return new PageResult<>(rows, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    private Range range(LocalDate from, LocalDate to) {
        LocalDate today = clock.today();
        LocalDate f = from != null ? from : today.minusMonths(11).withDayOfMonth(1);
        LocalDate t = to != null ? to : today.plusMonths(12);

        if (t.isBefore(f)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(f, t) > 800) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The date range can be at most about 2 years");
        }
        return new Range(f, t);
    }
}