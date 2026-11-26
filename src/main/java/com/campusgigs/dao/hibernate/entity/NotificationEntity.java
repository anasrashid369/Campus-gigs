package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.Notification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Row of the Notifications table. */
@Entity
@Table(name = "Notifications")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NotificationEntity() {
    }

    /** A new unread notification; the database fills in the id and creation time. */
    public NotificationEntity(long userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public long getNotificationId() {
        return notificationId;
    }

    public Notification toModel() {
        return new Notification(notificationId, userId, message, read, createdAt);
    }
}
