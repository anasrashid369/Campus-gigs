package com.campusgigs.service;

import com.campusgigs.dto.NotificationDto;

import java.util.List;

/**
 * In-app notifications (F4). Notifications are created by event handlers (Observer
 * pattern) when other services publish events on the EventBus; screens only read
 * them and mark them as read.
 */
public interface NotificationService {

    /** Longest message the Notifications table can hold; longer messages are shortened. */
    int MAX_MESSAGE_LENGTH = 500;

    /** Every notification of the user, newest first. */
    List<NotificationDto> getAll(long userId);

    /** The user's unread notifications, newest first. */
    List<NotificationDto> getUnread(long userId);

    /** How many unread notifications the user has, for the badge in the dashboard header. */
    int countUnread(long userId);

    /**
     * Marks one notification as read.
     *
     * @param actingUserId the logged-in user; must own the notification
     * @throws com.campusgigs.exception.ResourceNotFoundException if there is no such notification
     * @throws com.campusgigs.exception.ValidationException if it belongs to another user
     */
    void markAsRead(long actingUserId, long notificationId);

    /** Marks every notification of the user as read. */
    void markAllAsRead(long userId);
}
