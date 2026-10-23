package blinkstay.booking.serviceImpl;

import blinkstay.booking.dto.BookingResponseDto;
import blinkstay.booking.dto.CancellationPreviewDto;
import blinkstay.booking.entities.Booking;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.enums.CancelActor;
import blinkstay.booking.mapper.BookingMapper;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.booking.service.BookingCancellationService;
import blinkstay.booking.service.BookingRules;
import blinkstay.booking.service.CancellationPolicy;
import blinkstay.common.payment.BookingCancelledEvent;
import blinkstay.common.payment.PaymentRefunds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationServiceImpl implements BookingCancellationService {
    private final BookingRepository bookingRepo;
    private final BookingRules rules;
    private final CancellationPolicy policy;
    private final BookingMapper mapper;
    private final PaymentRefunds payments;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional(readOnly = true)
    public CancellationPreviewDto preview(UUID guestId, UUID bookingId) {
        Booking b = bookingRepo.findById(bookingId).filter(x -> x.getGuestId().equals(guestId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        boolean cancellable = policy.guestCanCancel(b);
        BigDecimal refund = cancellable ? policy.guestRefund(b, Instant.now()) : BigDecimal.ZERO;

        String message;
        if (!cancellable) {
            message = b.getStatus() == BookingStatus.CONFIRMED
                    ? "The stay has already started, so this booking can no longer be cancelled here."
                    : "This booking cannot be cancelled.";
        } else if (b.getStatus() == BookingStatus.PENDING_PAYMENT) {
            message = "This booking is not paid yet, so cancelling it costs nothing.";
        } else if (refund.signum() > 0) {
            message = "You will get a full refund of " + b.getCurrency() + " " + refund.toPlainString() + ".";
        } else {
            message = "Free cancellation has ended, so you will get no refund";
        }

        return CancellationPreviewDto.builder().bookingId(b.getId()).reference(b.getReference())
                .status(b.getStatus()).cancellable(cancellable).refundAmount(refund).currency(b.getCurrency())
                .freeCancellationUntil(
                        b.getStatus() == BookingStatus.CONFIRMED ? policy.freeCancellationUntil(b) : null)
                .message(message).build();
    }

    @Override
    @Transactional
    public BookingResponseDto cancelByGuest(UUID guestId, UUID bookingId, String reason) {
        return cancel(bookingId, guestId, CancelActor.GUEST, reason);
    }

    @Override
    @Transactional
    public BookingResponseDto cancelByHost(UUID hostId, UUID bookingId, String reason) {
        return cancel(bookingId, hostId, CancelActor.HOST, reason);
    }

    @Override
    @Transactional
    public BookingResponseDto cancelByAdmin(UUID adminId, UUID bookingId, String reason) {
        return cancel(bookingId, adminId, CancelActor.ADMIN, reason);
    }

    private BookingResponseDto cancel(UUID bookingId, UUID actorId, CancelActor actor, String rawReason) {
        String reason = rawReason == null || rawReason.isBlank() ? null : rawReason.trim();
        if (actor != CancelActor.GUEST && reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required");
        }

        // LOCK ORDER: payment row first, then booking row. PaymentEventProcessor uses
        // the same order, so the two
        // can never wait for each other.
        UUID paymentId = bookingRepo.findPaymentIdById(bookingId);
        if (paymentId != null) {
            payments.lockPayment(paymentId);
        }

        Booking b = bookingRepo.findByIdForUpdate(bookingId)
                .filter(x -> mayCancel(x, actorId, actor))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!Objects.equals(paymentId, b.getPaymentId())) {
            // a payment was confirmed between the two reads
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The booking just changed. Please try again.");
        }

        Instant now = Instant.now();
        LocalDate today = rules.today();
        boolean wasPaid = b.getStatus() == BookingStatus.CONFIRMED;
        BigDecimal refund = null; // null == nothing was paid

        switch (b.getStatus()) {
            case PENDING_PAYMENT -> {
                if (actor == CancelActor.HOST) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Only paid bookings can be cancelled by a host");
                }
            }
            case CONFIRMED -> {
                if (actor == CancelActor.GUEST) {
                    if (!today.isBefore(b.getCheckIn())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "The stay has already started. Please contact the host");
                    }
                    refund = policy.guestRefund(b, now);
                } else {
                    if (!b.getCheckOut().isAfter(today)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "This stay has already ended");
                    }
                    refund = b.getTotalAmount(); // host and admin cancellations always refund in full
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking cannot be cancelled: it " +
                    "is " + b.getStatus().name().toLowerCase().replace('_', ' '));
        }
        ;

        if (refund != null && refund.signum() > 0) {
            if (b.getPaymentId() == null) {
                throw new IllegalStateException("Confirmed booking " + b.getReference() + " has no payment");
            }
            refund = payments.refundInFull(b.getPaymentId(), "Booking " + b.getReference() + " was cancelled");
        }

        String storedReason = reason != null ? reason : "Cancelled by the guest";
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelledAt(now);
        b.setCancelledBy(actorId);
        b.setCancellationReason(reason);
        b.setRefundAmount(refund);

        Booking saved = bookingRepo.save(b);

        publisher.publishEvent(new BookingCancelledEvent(saved.getId(), saved.getReference(), saved.getGuestId(),
                saved.getHostId(), saved.getListingTitle(), saved.getCheckIn(), saved.getCheckOut(), actor.name(),
                storedReason, refund, saved.getCurrency(), wasPaid));

        log.info("Booking {} cancelled by {} ({}), refund {}", saved.getReference(), actor, actorId, refund);

        return mapper.toDto(saved, now);
    }

    private static boolean mayCancel(Booking b, UUID actorId, CancelActor actor) {
        return switch (actor) {
            case GUEST -> b.getGuestId().equals(actorId);
            case HOST -> b.getHostId().equals(actorId);
            case ADMIN -> true;
        };
    }
}
