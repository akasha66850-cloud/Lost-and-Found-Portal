package com.campus.lostfound.service;

import com.campus.lostfound.dto.NotificationDto;
import com.campus.lostfound.entity.Notification;
import com.campus.lostfound.entity.Role;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.exception.ForbiddenException;
import com.campus.lostfound.exception.ResourceNotFoundException;
import com.campus.lostfound.repository.NotificationRepository;
import com.campus.lostfound.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Notification createNotification(User user, String message, String type, Long relatedItemId) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .relatedItemId(relatedItemId)
                .read(false)
                .build();
        return notificationRepository.save(notification);
    }

    public void notifyStaff(String message, String type, Long relatedItemId) {
        List<User> staffMembers = userRepository.findByRole(Role.STAFF);
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User staff : staffMembers) {
            createNotification(staff, message, type, relatedItemId);
        }
        for (User admin : admins) {
            createNotification(admin, message, type, relatedItemId);
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot modify another user's notification");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndReadFalse(user);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    private NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .type(notification.getType())
                .relatedItemId(notification.getRelatedItemId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
