package com.campusgigs.presentation.common;

import com.campusgigs.service.NotificationService;
import com.campusgigs.util.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.util.Duration;

/**
 * The "Notifications (n)" button in the dashboard header (F4). It shows the unread count,
 * checks for new notifications every {@value #REFRESH_SECONDS} seconds while it is on
 * screen, and opens the notification list in a window when clicked.
 */
public class NotificationBadge extends BasePanel {

    static final int REFRESH_SECONDS = 20;
    private static final int WINDOW_WIDTH = 780;
    private static final int WINDOW_HEIGHT = 460;
    private static final String UNREAD = "badge-unread";

    private final NotificationService notificationService;
    private final Button button = new Button();
    private Timeline timer;

    public NotificationBadge(NotificationService notificationService) {
        this.notificationService = notificationService;
        useAsWidget();
        setCenter(button);
        showCount(0);
        button.setOnAction(event -> openList());
        // Check while on screen only: start when added to a window, stop when the dashboard goes.
        sceneProperty().addListener((observable, before, now) -> {
            if (now != null) {
                refresh();
                startTimer();
            } else if (timer != null) {
                timer.stop();
            }
        });
    }

    /** Reloads the unread count in the background. */
    public void refresh() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            return;
        }
        long userId = SessionManager.getInstance().getCurrentUser().userId();
        // A failed check is not worth an error dialog every 20 seconds; the next check tries again.
        runInBackground(() -> notificationService.countUnread(userId), this::showCount, error -> { });
    }

    /** The button text, for example "Notifications (3)". */
    String text() {
        return button.getText();
    }

    void showCount(int unread) {
        button.setText(unread == 0 ? "Notifications" : "Notifications (" + unread + ")");
        button.getStyleClass().remove(UNREAD);
        if (unread > 0) {
            button.getStyleClass().add(UNREAD);
        }
    }

    private void startTimer() {
        if (timer == null) {
            timer = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS), event -> refresh()));
            timer.setCycleCount(Animation.INDEFINITE);
        }
        timer.play();
    }

    private void openList() {
        Dialogs.modal(this, "Notifications", new NotificationsPanel(notificationService, this::refresh),
                WINDOW_WIDTH, WINDOW_HEIGHT).showAndWait();
        refresh();
    }
}
