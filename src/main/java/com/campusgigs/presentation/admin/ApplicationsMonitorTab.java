package com.campusgigs.presentation.admin;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.Choice;
import com.campusgigs.presentation.common.StatusFormat;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.MonitoringService;
import com.campusgigs.util.SessionManager;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/** All applications on the platform, filtered by status (F9). */
class ApplicationsMonitorTab extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final transient MonitoringService monitoringService;
    private final JComboBox<Choice<ApplicationStatus>> statusBox = new JComboBox<>();
    private final MonitorTable<ApplicationDto> table;

    ApplicationsMonitorTab(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        setLayout(new BorderLayout(0, UiConstants.SPACING_MEDIUM));
        statusBox.addItem(new Choice<>("All statuses", null));
        for (ApplicationStatus status : ApplicationStatus.values()) {
            statusBox.addItem(new Choice<>(StatusFormat.applicationStatus(status), status));
        }
        statusBox.addActionListener(e -> load());
        table = new MonitorTable<>("Applications", List.of("Gig", "Freelancer", "Application", "Gig status", "Sent"),
                ApplicationsMonitorTab::toCells, this::load);

        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, UiConstants.SPACING_MEDIUM, 0));
        filter.setOpaque(false);
        filter.add(new JLabel("Status:"));
        filter.add(statusBox);
        add(filter, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        load();
    }

    /** The cell values of one application, in column order. */
    static Object[] toCells(ApplicationDto application) {
        return new Object[] {
                application.gigTitle(),
                application.freelancerName(),
                StatusFormat.applicationStatus(application.status()),
                StatusFormat.gigStatus(application.gigStatus()),
                TokenFormat.dateTime(application.submittedAt())
        };
    }

    private void load() {
        long adminId = SessionManager.getInstance().getCurrentUser().userId();
        @SuppressWarnings("unchecked")
        Choice<ApplicationStatus> chosen = (Choice<ApplicationStatus>) statusBox.getSelectedItem();
        ApplicationStatus status = chosen == null ? null : chosen.value();
        runInBackground(() -> monitoringService.listApplications(adminId, status), table::show);
    }
}
