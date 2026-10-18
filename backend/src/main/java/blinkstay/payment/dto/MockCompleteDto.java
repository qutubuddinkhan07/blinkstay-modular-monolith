package blinkstay.payment.dto;

import blinkstay.payment.enums.MockOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MockCompleteDto {
    @NotNull(message = "outcome is required (SUCCESS or FAILURE")
    private MockOutcome outcome;
}
