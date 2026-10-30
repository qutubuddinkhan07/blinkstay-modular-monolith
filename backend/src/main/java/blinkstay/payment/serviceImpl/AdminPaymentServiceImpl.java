package blinkstay.payment.serviceImpl;

import blinkstay.payment.dto.AdminPaymentDto;
import blinkstay.payment.entities.Payment;
import blinkstay.payment.enums.PaymentStatus;
import blinkstay.payment.event.RefundRequestedEvent;
import blinkstay.payment.repository.PaymentRepository;
import blinkstay.payment.service.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentServiceImpl implements AdminPaymentService {
    private final PaymentRepository paymentRepo;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional(readOnly = true)
    public List<AdminPaymentDto> needingAttention() {
        return paymentRepo.findNeedingAttention(PageRequest.of(0, 50)).stream().map(AdminPaymentServiceImpl::toDto).toList();
    }

    @Override
    @Transactional
    public AdminPaymentDto retryRefund(UUID paymentId) {
        Payment p = paymentRepo.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (p.getStatus() != PaymentStatus.REFUND_FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a failed refund can be retried.");
        }

        p.setStatus(PaymentStatus.REFUND_PENDING);
        p.setRefundAttempts(0);
        p.setNextRefundAt(Instant.now());
        p.setRefundError(null);
        paymentRepo.save(p);

        publisher.publishEvent(new RefundRequestedEvent(p.getId())); // runs after this transaction commits
        log.info("Refund of payment {} again by an admin", p.getId());
        return toDto(p);
    }

    @Override
    @Transactional
    public AdminPaymentDto resolve(UUID paymentId) {
        Payment p = paymentRepo.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (p.getReviewReason() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This payment is not waiting for a review");
        }

        p.setReviewReason(null);
        paymentRepo.save(p);
        return toDto(p);
    }

    private static AdminPaymentDto toDto(Payment p) {
        return AdminPaymentDto.builder().paymentId(p.getId()).bookingId(p.getBookingId())
                .bookingReference(p.getBookingReference()).guestId(p.getGuestId()).provider(p.getProvider())
                .providerPaymentId(p.getProviderPaymentId()).amount(p.getAmount()).currency(p.getCurrency())
                .status(p.getStatus()).refundAttempts(p.getRefundAttempts()).refundError(p.getRefundError())
                .reviewReason(p.getReviewReason()).updatedAt(p.getUpdatedAt()).build();
    }
}
