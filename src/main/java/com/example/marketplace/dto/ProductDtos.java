package com.example.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class ProductDtos {
    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull @DecimalMin(value = "0.01") BigDecimal price,
            @Min(0) int stockQuantity,
            @NotNull Long businessId
    ) {
    }

    public record ProductResponse(
            Long id,
            String name,
            BigDecimal price,
            int stockQuantity,
            Long businessId,
            String businessName
    ) {
    }
}
