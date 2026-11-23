package com.campusgigs.presentation.admin;

import com.campusgigs.dto.GigDto;
import com.campusgigs.model.GigStatus;
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

/** All gigs on the platform, filtered by status (F9). */
class GigsMonitorTab extends BasePanel {

    private static final long serialVersionUID = 1L;
    private static final String NO_VALUE = "-";

    private final transient MonitoringService monitoringService;
    private final JComboBox<Choice<GigStatus>> statusBox = new JComboBox<>();
    private final MonitorTable<GigDto> table;

    GigsMonitorTab(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        setLayout(new BorderLayout(0, UiConstants.SPACING_MEDIUM));
        statusBox.addItem(new Choice<>("All statuses", null));
        for (GigStatus status : GigStatus.values()) {
            statusBox.addItem(new Choice<>(StatusFormat.gigStatus(status), status));
        }
        statusBox.addActionListener(e -> load());
        table = new MonitorTable<>("Gigs", List.of("Title", "Budget", "Deadline", "Status", "Client", "Freelancer"),
                GigsMonitorTab::toCells, this::load);

        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, UiConstants.SPACING_MEDIUM, 0));
        filter.setOpaque(false);
        filter.add(new JLabel("Status:"));
        filter.add(statusBox);
        add(filter, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        load();
    }

    /** The cell values of one gig, in column order. */
    static Object[] toCells(GigDto gig) {
        return new Object[] {
                gig.title(),
                TokenFormat.tokens(gig.budget()),
                gig.deadline(),
                StatusFormat.gigStatus(gig.status()),
                "#" + gig.clientId(),
                gig.assignedFreelancerId() == null ? NO_VALUE : "#" + gig.assignedFreelancerId()
        };
    }

    private void load() {
        long adminId = SessionManager.getInstance().getCurrentUser().userId();
        @SuppressWarnings("unchecked")
        Choice<GigStatus> chosen = (Choice<GigStatus>) statusBox.getSelectedItem();
        GigStatus status = chosen == null ? null : chosen.value();
        runInBackground(() -> monitoringService.listGigs(adminId, status), table::show);
    }
}
