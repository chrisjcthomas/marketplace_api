package com.example.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 255) String fullName,
            @Email @NotBlank @Size(max = 255) String email
    ) {
    }

    public record UserResponse(Long id, String fullName, String email) {
    }
}
