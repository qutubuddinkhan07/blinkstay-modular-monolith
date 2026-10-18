package blinkstay.payment.controller;

import blinkstay.payment.dto.MockCompleteDto;
import blinkstay.payment.dto.PaymentApiResponse;
import blinkstay.payment.dto.PaymentResultDto;
import blinkstay.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v5/payments/mock")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentController {
    private final PaymentService paymentService;

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<PaymentApiResponse<PaymentResultDto>> complete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("orderId") String orderId, @Valid @RequestBody MockCompleteDto dto) {
        PaymentResultDto result = paymentService.completeMock(UUID.fromString(userDetails.getUsername()), orderId,
                dto.getOutcome());
        return ResponseEntity.ok(PaymentApiResponse.success(result.getMessage(), result));
    }
}
