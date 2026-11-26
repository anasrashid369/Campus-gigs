package com.campusgigs.presentation.common;

import com.campusgigs.dto.NotificationDto;
import com.campusgigs.service.NotificationService;
import com.campusgigs.util.SessionManager;

import java.util.List;

/** The logged-in user's notifications (F4), newest first, with mark as read. */
public class NotificationsPanel extends TablePanel<NotificationDto> {

    private static final long serialVersionUID = 1L;
    private static final int RECEIVED_WIDTH = 130;
    private static final int MESSAGE_WIDTH = 460;
    private static final int STATUS_WIDTH = 60;

    private final transient NotificationService notificationService;
    private final transient Runnable onChange;

    /**
     * @param onChange called on the EDT after notifications were marked as read, so the
     *                 header badge can refresh its count
     */
    public NotificationsPanel(NotificationService notificationService, Runnable onChange) {
        super("Notifications", List.of("Received", "Message", "Status"),
                n -> new Object[] {TokenFormat.dateTime(n.createdAt()), n.message(), n.read() ? "Read" : "New"});
        this.notificationService = notificationService;
        this.onChange = onChange;
        setColumnWidths(RECEIVED_WIDTH, MESSAGE_WIDTH, STATUS_WIDTH);
        addAction("Mark as read", true, this::markSelectedAsRead);
        addAction("Mark all as read", false, this::markAllAsRead);
        addAction("Refresh", false, this::load);
        load();
    }

    private void load() {
        long userId = currentUserId();
        runInBackground(() -> notificationService.getAll(userId), this::setRows);
    }

    private void markSelectedAsRead() {
        selected().ifPresent(notification -> {
            long userId = currentUserId();
            runInBackground(() -> {
                notificationService.markAsRead(userId, notification.notificationId());
                return null;
            }, done -> changed());
        });
    }

    private void markAllAsRead() {
        long userId = currentUserId();
        runInBackground(() -> {
            notificationService.markAllAsRead(userId);
            return null;
        }, done -> changed());
    }

    private void changed() {
        load();
        onChange.run();
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
