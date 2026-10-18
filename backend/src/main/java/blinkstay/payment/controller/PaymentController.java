package blinkstay.payment.controller;

import blinkstay.payment.dto.PaymentApiResponse;
import blinkstay.payment.dto.PaymentSessionDto;
import blinkstay.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v5")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/bookings/{bookingId}/pay")
    public ResponseEntity<PaymentApiResponse<PaymentSessionDto>> pay(@AuthenticationPrincipal UserDetails userDetails
            , @PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(PaymentApiResponse.success("Payment started",
                paymentService.startPayment(UUID.fromString(userDetails.getUsername()), bookingId)));
    }
}
