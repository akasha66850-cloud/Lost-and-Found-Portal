package com.example.lostfound.service;

import com.example.lostfound.model.Notification;
import com.example.lostfound.model.User;
import com.example.lostfound.repository.NotificationRepository;
import com.example.lostfound.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    public NotificationService(NotificationRepository notificationRepo, UserRepository userRepo) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
    }

    public Notification createNotification(User recipient, String message, String type, Long relatedId) {
        if (recipient == null) return null;
        Notification n = Notification.builder()
                .recipient(recipient)
                .message(message)
                .type(type)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        return notificationRepo.save(n);
    }

    public List<Notification> getMyNotifications(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return notificationRepo.findByRecipientOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return notificationRepo.countByRecipientAndIsReadFalse(user);
    }

    public void markRead(Long notificationId, String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        Notification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!n.getRecipient().getId().equals(user.getId())) {
            throw new SecurityException("Access denied: notification does not belong to this user");
        }
        n.setRead(true);
        notificationRepo.save(n);
    }

    public void markAllRead(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        List<Notification> unread = notificationRepo.findByRecipientAndIsReadFalse(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepo.saveAll(unread);
    }
}
