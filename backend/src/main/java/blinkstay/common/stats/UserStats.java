package blinkstay.common.stats;

import java.time.LocalDate;

public interface UserStats {
    UserCounts counts(LocalDate from, LocalDate to);
}