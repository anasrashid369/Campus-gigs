package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.UserDto;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.presentation.common.DashboardPanel;

/** Home screen for a FREELANCER. Its screens are added through the MainFrame navigation list. */
public class FreelancerDashboardPanel extends DashboardPanel {

    public FreelancerDashboardPanel(UserDto user, MainFrame frame) {
        super(user, frame, "Find gigs, track your applications and get paid.");
    }
}
