package com.campusgigs.service.impl;

import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.NotificationDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.NotificationDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.Notification;
import com.campusgigs.model.User;
import com.campusgigs.pattern.observer.ApplicationSubmitted;
import com.campusgigs.pattern.observer.ApplicationWithdrawn;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.service.NotificationService;

import java.util.List;

/**
 * Observer side of the notifications: {@link #subscribeTo(EventBus)} registers one handler
 * per event, and each handler saves a readable message for the user who should know.
 * Handlers run on the publisher's thread after its transaction has committed.
 */
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notificationDao;
    private final UserDao userDao;
    private final GigDao gigDao;

    public NotificationServiceImpl(NotificationDao notificationDao, UserDao userDao, GigDao gigDao) {
        this.notificationDao = notificationDao;
        this.userDao = userDao;
        this.gigDao = gigDao;
    }

    /** Registers the notification handlers. Call once at startup with the shared bus. */
    public void subscribeTo(EventBus bus) {
        bus.subscribe(ApplicationSubmitted.class, this::onApplicationSubmitted);
        bus.subscribe(ApplicationWithdrawn.class, this::onApplicationWithdrawn);
    }

    void onApplicationSubmitted(ApplicationSubmitted event) {
        send(event.clientId(), freelancerName(event.freelancerId()) + " applied to your gig "
                + gigTitle(event.gigId()) + ".");
    }

    void onApplicationWithdrawn(ApplicationWithdrawn event) {
        send(event.clientId(), freelancerName(event.freelancerId()) + " withdrew their application for your gig "
                + gigTitle(event.gigId()) + ".");
    }

    @Override
    public List<NotificationDto> getAll(long userId) {
        return notificationDao.findByUserId(userId).stream().map(NotificationServiceImpl::toDto).toList();
    }

    @Override
    public List<NotificationDto> getUnread(long userId) {
        return notificationDao.findUnreadByUserId(userId).stream().map(NotificationServiceImpl::toDto).toList();
    }

    @Override
    public int countUnread(long userId) {
        return notificationDao.countUnread(userId);
    }

    @Override
    public void markAsRead(long actingUserId, long notificationId) {
        Notification notification = notificationDao.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.userId() != actingUserId) {
            throw new ValidationException("You can only mark your own notifications as read");
        }
        if (!notification.read()) {
            notificationDao.markAsRead(notificationId);
        }
    }

    @Override
    public void markAllAsRead(long userId) {
        notificationDao.markAllAsRead(userId);
    }

    private void send(long userId, String message) {
        String text = message.length() <= MAX_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
        notificationDao.insert(userId, text);
    }

    private String freelancerName(long userId) {
        return userDao.findById(userId).map(User::fullName).orElse("A freelancer");
    }

    private String gigTitle(long gigId) {
        return gigDao.findById(gigId).map(gig -> "\"" + gig.title() + "\"").orElse("#" + gigId);
    }

    private static NotificationDto toDto(Notification notification) {
        return new NotificationDto(notification.notificationId(), notification.message(), notification.read(),
                notification.createdAt());
    }
}
