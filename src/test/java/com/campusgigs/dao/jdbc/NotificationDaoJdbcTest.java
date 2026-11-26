package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.model.Notification;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class NotificationDaoJdbcTest {

    private ConnectionProvider provider;
    private NotificationDaoJdbc notifications;
    private long userId;
    private long otherUserId;

    @BeforeEach
    void setUp() throws Exception {
        provider = DbTestSupport.provider();
        notifications = new NotificationDaoJdbc(provider);
        userId = DbTestSupport.createUser(provider);
        otherUserId = DbTestSupport.createUser(provider);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider == null) {
            return;
        }
        DbTestSupport.deleteUser(provider, userId);
        DbTestSupport.deleteUser(provider, otherUserId);
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
