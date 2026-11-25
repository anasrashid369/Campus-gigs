package com.campusgigs.dao;

import com.campusgigs.model.Notification;

import java.util.List;
import java.util.Optional;

/** Data access for in-app notifications (table Notifications). */
public interface NotificationDao {

    /** Saves a new unread notification for the user and returns its id. */
    long insert(long userId, String message);

    /** The notification with this id, if it exists. */
    Optional<Notification> findById(long notificationId);

    /** Every notification of the user, newest first. */
    List<Notification> findByUserId(long userId);

    /** The user's unread notifications, newest first. */
    List<Notification> findUnreadByUserId(long userId);

    /** How many unread notifications the user has. */
    int countUnread(long userId);

    /** Marks one notification as read. */
    void markAsRead(long notificationId);

    /** Marks every notification of the user as read. */
    void markAllAsRead(long userId);
}
