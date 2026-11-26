package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.model.Notification;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class NotificationDaoHibernateTest {

    private SessionFactory db;
    private NotificationDaoHibernate notifications;
    private long userId;
    private long otherUserId;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        notifications = new NotificationDaoHibernate(db);
        userId = DbTestSupport.createUser(db);
        otherUserId = DbTestSupport.createUser(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db == null) {
            return;
        }
        DbTestSupport.deleteUser(db, userId);
        DbTestSupport.deleteUser(db, otherUserId);
    }

    @Test
    void savesAndListsNewestFirst() {
        long first = notifications.insert(userId, "First");
        long second = notifications.insert(userId, "Second");
        notifications.insert(otherUserId, "Not yours");

        List<Notification> all = notifications.findByUserId(userId);

        assertEquals(List.of(second, first), all.stream().map(Notification::notificationId).toList());
        assertFalse(all.get(0).read());
        assertEquals("Second", notifications.findById(second).orElseThrow().message());
    }

    @Test
    void marksOneOrAllAsRead() {
        long first = notifications.insert(userId, "First");
        notifications.insert(userId, "Second");
        long other = notifications.insert(otherUserId, "Not yours");
        assertEquals(2, notifications.countUnread(userId));

        notifications.markAsRead(first);
        assertEquals(1, notifications.countUnread(userId));
        assertEquals(1, notifications.findUnreadByUserId(userId).size());

        notifications.markAllAsRead(userId);
        assertEquals(0, notifications.countUnread(userId));
        assertTrue(notifications.findUnreadByUserId(userId).isEmpty());
        assertFalse(notifications.findById(other).orElseThrow().read());
    }
}
