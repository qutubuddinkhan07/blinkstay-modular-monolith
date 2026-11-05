package blinkstay.earnings.repository;

import blinkstay.earnings.dto.ListingEarningsDto;
import blinkstay.earnings.dto.MonthlyEarningsDto;
import blinkstay.earnings.dto.StatusTotals;
import blinkstay.earnings.entities.EarningsEntry;
import blinkstay.earnings.enums.EarningsStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EarningsRepository extends JpaRepository<EarningsEntry, UUID> {

    boolean existsByBookingId(UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EarningsEntry e where e.bookingId = :bookingId")
    Optional<EarningsEntry> findByBookingIdForUpdate(@Param("bookingId") UUID bookingId);

    /**
     * Stays that are over: PENDING becomes EARNED.
     */
    @Modifying(clearAutomatically = true)
    @Query("update EarningsEntry e set e.status = blinkstay.earnings.enums.EarningsStatus.EARNED,"
            + " e.earnedAt = :now, e.version = e.version + 1, e.updatedAt = :now"
            + " where e.status = blinkstay.earnings.enums.EarningsStatus.PENDING and e.checkOut < :today")
    int markEarned(@Param("today") LocalDate today, @Param("now") Instant now);

    @Query("select new blinkstay.earnings.dto.StatusTotals(e.status, count(e), sum(e.gross), sum(e.refunded),"
            + " sum(e.commission), sum(e.hostAmount)) from EarningsEntry e"
            + " where e.hostId = :hostId and e.checkIn between :from and :to and e.currency = :currency"
            + " group by e.status")
    List<StatusTotals> totalsByStatus(@Param("hostId") UUID hostId, @Param("from") LocalDate from,
                                      @Param("to") LocalDate to, @Param("currency") String currency);

    @Query("select new blinkstay.earnings.dto.ListingEarningsDto(e.listingId, max(e.listingTitle), count(e),"
            + " sum(e.gross), sum(e.hostAmount)) from EarningsEntry e"
            + " where e.hostId = :hostId and e.checkIn between :from and :to and e.currency = :currency"
            + " and e.status <> blinkstay.earnings.enums.EarningsStatus.REVERSED"
            + " group by e.listingId order by sum(e.hostAmount) desc")
    List<ListingEarningsDto> byListing(@Param("hostId") UUID hostId, @Param("from") LocalDate from,
                                       @Param("to") LocalDate to, @Param("currency") String currency);

    @Query("select new blinkstay.earnings.dto.MonthlyEarningsDto(extract(year from e.checkIn),"
            + " extract(month from e.checkIn), count(e), sum(e.gross), sum(e.hostAmount)) from EarningsEntry e"
            + " where e.hostId = :hostId and e.checkIn between :from and :to and e.currency = :currency"
            + " and e.status <> blinkstay.earnings.enums.EarningsStatus.REVERSED"
            + " group by extract(year from e.checkIn), extract(month from e.checkIn)"
            + " order by extract(year from e.checkIn), extract(month from e.checkIn)")
    List<MonthlyEarningsDto> byMonth(@Param("hostId") UUID hostId, @Param("from") LocalDate from,
                                     @Param("to") LocalDate to, @Param("currency") String currency);

    Page<EarningsEntry> findByHostIdAndCheckInBetween(UUID hostId, LocalDate from, LocalDate to, Pageable pageable);

    Page<EarningsEntry> findByHostIdAndStatusAndCheckInBetween(UUID hostId, EarningsStatus status, LocalDate from,
                                                               LocalDate to, Pageable pageable);
}