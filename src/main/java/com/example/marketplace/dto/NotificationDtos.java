package com.example.marketplace.dto;

import com.example.marketplace.domain.NotificationType;
import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            Long userId,
            NotificationType type,
            String message,
            Instant createdAt
    ) {
    }
}
