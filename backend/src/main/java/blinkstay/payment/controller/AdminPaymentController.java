package blinkstay.payment.serviceImpl;

import blinkstay.payment.dto.AdminPaymentDto;
import blinkstay.payment.dto.PaymentApiResponse;
import blinkstay.payment.service.AdminPaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v5/admin/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminPaymentController {
    private final AdminPaymentService service;

    @GetMapping("/attention")
    public ResponseEntity<PaymentApiResponse<List<AdminPaymentDto>>> attention() {
        return ResponseEntity.ok(PaymentApiResponse.success("Payment needing attention", service.needingAttention()));
    }

    @PostMapping("/{paymentId}/retry-refund")
    public ResponseEntity<PaymentApiResponse<AdminPaymentDto>> retryRefund(@PathVariable("paymentId") UUID paymentId) {
        return ResponseEntity.ok(PaymentApiResponse.success("Refund queue again", service.retryRefund(paymentId)));
    }

    @PostMapping("/{paymentId}/resolve")
    public ResponseEntity<PaymentApiResponse<AdminPaymentDto>> resolve(@PathVariable("paymentId") UUID paymentId) {
        return ResponseEntity.ok(PaymentApiResponse.success("Marked as resolved", service.resolve(paymentId)));
    }
}
