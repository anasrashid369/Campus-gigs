package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.campusgigs.service.NotificationService;
import com.campusgigs.util.SessionManager;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NotificationBadgeTest {

    private final NotificationService service = mock(NotificationService.class);

    @AfterEach
    void logout() {
        SessionManager.getInstance().clear();
    }

    @Test
    void showsTheUnreadCount() throws Exception {
        String[] texts = new String[2];
        SwingUtilities.invokeAndWait(() -> {
            NotificationBadge badge = new NotificationBadge(service);
            badge.showCount(3);
            texts[0] = badge.text();
            badge.showCount(0);
            texts[1] = badge.text();
        });

        assertEquals("Notifications (3)", texts[0]);
        assertEquals("Notifications", texts[1]);
    }

    @Test
    void doesNotAskTheServiceWhenNobodyIsLoggedIn() throws Exception {
        SessionManager.getInstance().clear();

        SwingUtilities.invokeAndWait(() -> new NotificationBadge(service).refresh());

        verifyNoInteractions(service);
    }
}
