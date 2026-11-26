package com.campusgigs.presentation.common;

import com.campusgigs.service.NotificationService;
import com.campusgigs.util.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;

/**
 * The "Notifications (n)" button in the dashboard header (F4). It shows the unread count,
 * checks for new notifications every {@value #REFRESH_SECONDS} seconds while it is on
 * screen, and opens the notification list in a dialog when clicked.
 */
public class NotificationBadge extends BasePanel {

    private static final long serialVersionUID = 1L;

    static final int REFRESH_SECONDS = 20;
    private static final int DIALOG_WIDTH = 720;
    private static final int DIALOG_HEIGHT = 440;

    private final transient NotificationService notificationService;
    private final JButton button = new JButton();
    private final Timer timer = new Timer(REFRESH_SECONDS * 1000, e -> refresh());
    private final Font normalFont = button.getFont();

    public NotificationBadge(NotificationService notificationService) {
        this.notificationService = notificationService;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        add(button, BorderLayout.CENTER);
        showCount(0);
        button.addActionListener(e -> openList());
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
        button.setFont(unread == 0 ? normalFont : normalFont.deriveFont(Font.BOLD));
        button.setForeground(unread == 0 ? UiConstants.TEXT : UiConstants.PRIMARY);
    }

    private void openList() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Notifications",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(new NotificationsPanel(notificationService, this::refresh));
        dialog.setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        refresh();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refresh();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }
}
