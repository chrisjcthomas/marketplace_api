package com.example.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class BusinessDtos {
    private BusinessDtos() {
    }

    public record CreateBusinessRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 255) String category,
            @NotNull Long ownerUserId
    ) {
    }

    public record BusinessResponse(Long id, String name, String category, Long ownerUserId) {
    }
}
