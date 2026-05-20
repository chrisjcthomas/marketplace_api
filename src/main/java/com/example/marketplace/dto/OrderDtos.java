package com.example.marketplace.dto;

import com.example.marketplace.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {
    }

    public record CreateOrderRequest(
            @NotNull Long customerUserId,
            @NotEmpty @Size(max = 100) List<@Valid OrderItemRequest> items
    ) {
    }

    public record OrderItemRequest(
            @NotNull Long productId,
            @Min(1) int quantity
    ) {
    }

    public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
    }

    public record OrderResponse(
            Long id,
            Long customerUserId,
            OrderStatus status,
            BigDecimal totalAmount,
            Instant createdAt,
            List<OrderItemResponse> items
    ) {
    }

    public record OrderItemResponse(
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
