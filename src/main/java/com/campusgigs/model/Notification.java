package com.campusgigs.model;

import java.time.LocalDateTime;

/** An in-app notification (row of Notifications). */
public record Notification(long notificationId, long userId, String message,
                           boolean read, LocalDateTime createdAt) {
}
