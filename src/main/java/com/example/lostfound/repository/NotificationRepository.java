package com.example.lostfound.repository;

import com.example.lostfound.model.Notification;
import com.example.lostfound.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndIsReadFalse(User recipient);

    List<Notification> findByRecipientAndIsReadFalse(User recipient);

    List<Notification> findAllByOrderByCreatedAtDesc();
}
