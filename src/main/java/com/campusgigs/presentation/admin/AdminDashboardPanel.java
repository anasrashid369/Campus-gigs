package com.campusgigs.presentation.admin;

import com.campusgigs.dto.UserDto;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.presentation.common.DashboardPanel;

/** Home screen for an ADMIN. Its screens are added through the MainFrame navigation list. */
public class AdminDashboardPanel extends DashboardPanel {

    private static final long serialVersionUID = 1L;

    public AdminDashboardPanel(UserDto user, MainFrame frame) {
        super(user, frame, "Manage users, reference data, settings and disputes.");
    }
}
