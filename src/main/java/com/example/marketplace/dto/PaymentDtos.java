package com.example.marketplace.dto;

import com.example.marketplace.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    public record SimulatePaymentRequest(
            @NotNull Long orderId,
            boolean approved
    ) {
    }

    public record PaymentResponse(
            Long id,
            Long orderId,
            BigDecimal amount,
            PaymentStatus status,
            Instant simulatedAt
    ) {
    }
}
