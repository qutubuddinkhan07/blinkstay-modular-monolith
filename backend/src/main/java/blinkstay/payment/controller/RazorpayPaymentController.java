package blinkstay.payment.controller;

import blinkstay.payment.dto.PaymentApiResponse;
import blinkstay.payment.dto.PaymentResultDto;
import blinkstay.payment.dto.RazorpayVerifyDto;
import blinkstay.payment.serviceImpl.RazorpayCheckoutService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v5/payments/razorpay")
@Validated
@SecurityRequirement(name = "Bearer Authentication")
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "razorpay")
public class RazorpayPaymentController {
    private final RazorpayCheckoutService checkout;

    public ResponseEntity<PaymentApiResponse<PaymentResultDto>> verify(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("orderId") String orderId, @Valid @RequestBody RazorpayVerifyDto dto) {
        PaymentResultDto result = checkout.verify(UUID.fromString(userDetails.getUsername()), orderId, dto);
        return ResponseEntity.ok(PaymentApiResponse.success(result.getMessage(), result));
    }
}
