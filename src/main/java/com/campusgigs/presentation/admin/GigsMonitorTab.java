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
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** All gigs on the platform, filtered by status (F9). */
class GigsMonitorTab extends BasePanel {

    private static final String NO_VALUE = "-";

    private final MonitoringService monitoringService;
    private final ComboBox<Choice<GigStatus>> statusBox = new ComboBox<>();
    private final MonitorTable<GigDto> table;

    GigsMonitorTab(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        statusBox.getItems().add(new Choice<>("All statuses", null));
        for (GigStatus status : GigStatus.values()) {
            statusBox.getItems().add(new Choice<>(StatusFormat.gigStatus(status), status));
        }
        statusBox.getSelectionModel().selectFirst();
        statusBox.setOnAction(event -> load());
        table = new MonitorTable<>("Gigs", List.of("Title", "Budget", "Deadline", "Status", "Client", "Freelancer"),
                GigsMonitorTab::toCells, this::load);

        HBox filter = new HBox(UiConstants.SPACING_MEDIUM, new Label("Status:"), statusBox);
        filter.setAlignment(Pos.CENTER_LEFT);
        setMargin(filter, new Insets(0, 0, UiConstants.SPACING_MEDIUM, 0));
        setTop(filter);
        setCenter(table);
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
        Choice<GigStatus> chosen = statusBox.getValue();
        GigStatus status = chosen == null ? null : chosen.value();
        runInBackground(() -> monitoringService.listGigs(adminId, status), table::show);
    }
}
