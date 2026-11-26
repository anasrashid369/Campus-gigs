package com.campusgigs.dto;

import java.time.LocalDateTime;

/** One in-app notification as the screens show it. */
public record NotificationDto(long notificationId, String message, boolean read, LocalDateTime createdAt) {
}
