package com.campusgigs.presentation.client;

import com.campusgigs.dto.UserDto;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.presentation.common.DashboardPanel;

/** Home screen for a CLIENT. Its screens are added through the MainFrame navigation list. */
public class ClientDashboardPanel extends DashboardPanel {

    private static final long serialVersionUID = 1L;

    public ClientDashboardPanel(UserDto user, MainFrame frame) {
        super(user, frame, "Post gigs, hire freelancers and manage your tokens.");
    }
}
