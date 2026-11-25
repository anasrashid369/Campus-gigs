package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.NotificationDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.NotificationDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Notification;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.pattern.observer.ApplicationSubmitted;
import com.campusgigs.pattern.observer.ApplicationWithdrawn;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.service.NotificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final long CLIENT_ID = 3;
    private static final long FREELANCER_ID = 4;
    private static final long GIG_ID = 20;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 10, 30);

    @Mock NotificationDao notificationDao;
    @Mock UserDao userDao;
    @Mock GigDao gigDao;

    private final EventBus bus = new EventBus();
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationDao, userDao, gigDao);
        service.subscribeTo(bus);
        lenient().when(userDao.findById(FREELANCER_ID)).thenReturn(Optional.of(new User(FREELANCER_ID,
                Role.FREELANCER, "Sara Khan", "sara@uni.edu.pk", "pbkdf2$x", AccountStatus.ACTIVE, NOW)));
        lenient().when(gigDao.findById(GIG_ID)).thenReturn(Optional.of(gig("Logo design")));
    }

    private static Gig gig(String title) {
        return new Gig(GIG_ID, CLIENT_ID, 1, null, title, "A logo", 300, LocalDate.of(2026, 12, 10),
                GigStatus.OPEN, NOW);
    }

    // ---- event handlers ----

    @Test
    void clientIsToldAboutANewApplication() {
        bus.publish(new ApplicationSubmitted(7, GIG_ID, CLIENT_ID, FREELANCER_ID));

        verify(notificationDao).insert(CLIENT_ID, "Sara Khan applied to your gig \"Logo design\".");
    }

    @Test
    void clientIsToldAboutAWithdrawnApplication() {
        bus.publish(new ApplicationWithdrawn(7, GIG_ID, CLIENT_ID, FREELANCER_ID));

        verify(notificationDao).insert(CLIENT_ID,
                "Sara Khan withdrew their application for your gig \"Logo design\".");
    }

    @Test
    void missingUserOrGigStillGivesAReadableMessage() {
        when(userDao.findById(99)).thenReturn(Optional.empty());
        when(gigDao.findById(55)).thenReturn(Optional.empty());

        bus.publish(new ApplicationSubmitted(7, 55, CLIENT_ID, 99));

        verify(notificationDao).insert(CLIENT_ID, "A freelancer applied to your gig #55.");
    }

    @Test
    void longMessagesAreShortenedToFitTheColumn() {
        when(gigDao.findById(GIG_ID)).thenReturn(Optional.of(gig("x".repeat(600))));

        bus.publish(new ApplicationSubmitted(7, GIG_ID, CLIENT_ID, FREELANCER_ID));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationDao).insert(eq(CLIENT_ID), message.capture());
        assertEquals(NotificationService.MAX_MESSAGE_LENGTH, message.getValue().length());
        assertTrue(message.getValue().endsWith("..."));
    }

    // ---- reading ----

    @Test
    void listsNotificationsAsDtos() {
        Notification unread = new Notification(2, CLIENT_ID, "New", false, NOW);
        Notification read = new Notification(1, CLIENT_ID, "Old", true, NOW.minusDays(1));
        when(notificationDao.findByUserId(CLIENT_ID)).thenReturn(List.of(unread, read));
        when(notificationDao.findUnreadByUserId(CLIENT_ID)).thenReturn(List.of(unread));
        when(notificationDao.countUnread(CLIENT_ID)).thenReturn(1);

        assertEquals(List.of(new NotificationDto(2, "New", false, NOW),
                new NotificationDto(1, "Old", true, NOW.minusDays(1))), service.getAll(CLIENT_ID));
        assertEquals(List.of(new NotificationDto(2, "New", false, NOW)), service.getUnread(CLIENT_ID));
        assertEquals(1, service.countUnread(CLIENT_ID));
    }

    // ---- marking as read ----

    @Test
    void ownerMarksANotificationAsRead() {
        when(notificationDao.findById(2)).thenReturn(Optional.of(new Notification(2, CLIENT_ID, "New", false, NOW)));

        service.markAsRead(CLIENT_ID, 2);

        verify(notificationDao).markAsRead(2);
    }

    @Test
    void alreadyReadNotificationIsLeftAlone() {
        when(notificationDao.findById(2)).thenReturn(Optional.of(new Notification(2, CLIENT_ID, "New", true, NOW)));

        service.markAsRead(CLIENT_ID, 2);

        verify(notificationDao, never()).markAsRead(anyLong());
    }

    @Test
    void cannotMarkSomeoneElsesNotification() {
        when(notificationDao.findById(2)).thenReturn(Optional.of(new Notification(2, CLIENT_ID, "New", false, NOW)));

        assertThrows(ValidationException.class, () -> service.markAsRead(FREELANCER_ID, 2));
        verify(notificationDao, never()).markAsRead(anyLong());
    }

    @Test
    void unknownNotificationIsReported() {
        when(notificationDao.findById(404)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(CLIENT_ID, 404));
    }

    @Test
    void marksAllOfTheUsersNotificationsAsRead() {
        service.markAllAsRead(CLIENT_ID);

        verify(notificationDao).markAllAsRead(CLIENT_ID);
        verify(notificationDao, never()).insert(anyLong(), anyString());
    }
}
