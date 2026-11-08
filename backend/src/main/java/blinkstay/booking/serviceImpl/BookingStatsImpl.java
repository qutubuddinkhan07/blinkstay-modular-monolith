package blinkstay.booking.serviceImpl;

import blinkstay.booking.config.BookingProperties;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.common.stats.BookingStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingStatsImpl implements BookingStats {
    private final BookingRepository bookingRepo;
    private final BookingProperties props;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(props.zoneId()).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(props.zoneId()).toInstant();

        Map<String, Long> result = new LinkedHashMap<>();
        for (BookingStatus s : BookingStatus.values()) {
            result.put(s.name(), 0L);
        }
        for (Object[] row : bookingRepo.countByStatusBetween(start, end)) {
            result.put(((BookingStatus) row[0]).name(), (Long) row[1]);
        }
        return result;
    }
}
