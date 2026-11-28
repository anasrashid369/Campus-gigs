package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.campusgigs.presentation.FxTestSupport;
import com.campusgigs.service.NotificationService;
import com.campusgigs.util.SessionManager;
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
        String[] texts = FxTestSupport.onFxThread(() -> {
            NotificationBadge badge = new NotificationBadge(service);
            badge.showCount(3);
            String withUnread = badge.text();
            badge.showCount(0);
            return new String[] {withUnread, badge.text()};
        });

        assertEquals("Notifications (3)", texts[0]);
        assertEquals("Notifications", texts[1]);
    }

    @Test
    void doesNotAskTheServiceWhenNobodyIsLoggedIn() throws Exception {
        SessionManager.getInstance().clear();

        FxTestSupport.onFxThread(() -> {
            new NotificationBadge(service).refresh();
            return null;
        });

        verifyNoInteractions(service);
    }
}
