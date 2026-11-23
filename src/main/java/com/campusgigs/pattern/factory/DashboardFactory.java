package com.campusgigs.pattern.factory;

import com.campusgigs.dto.UserDto;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.presentation.admin.AdminDashboardPanel;
import com.campusgigs.presentation.client.ClientDashboardPanel;
import com.campusgigs.presentation.freelancer.FreelancerDashboardPanel;

import javax.swing.JPanel;
import java.util.Objects;

/**
 * Factory pattern: picks the dashboard that matches the user's role, so the login
 * code never needs to know which dashboard classes exist.
 */
public final class DashboardFactory {

    private DashboardFactory() {
    }

    /** Creates the dashboard for the user's role. Call on the Event Dispatch Thread. */
    public static JPanel create(UserDto user, MainFrame frame) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(frame, "frame");
        return switch (user.role()) {
            case CLIENT -> new ClientDashboardPanel(user, frame);
            case FREELANCER -> new FreelancerDashboardPanel(user, frame);
            case ADMIN -> new AdminDashboardPanel(user, frame);
        };
    }
}
