package com.example.marketplace.service;

import com.example.marketplace.domain.Notification;
import com.example.marketplace.domain.NotificationType;
import com.example.marketplace.domain.UserAccount;
import com.example.marketplace.dto.NotificationDtos.NotificationResponse;
import com.example.marketplace.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void create(UserAccount user, NotificationType type, String message) {
        notificationRepository.save(new Notification(user, type, message));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUser().getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getCreatedAt()
        );
    }
}
