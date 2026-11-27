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
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** All applications on the platform, filtered by status (F9). */
class ApplicationsMonitorTab extends BasePanel {

    private final MonitoringService monitoringService;
    private final ComboBox<Choice<ApplicationStatus>> statusBox = new ComboBox<>();
    private final MonitorTable<ApplicationDto> table;

    ApplicationsMonitorTab(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        statusBox.getItems().add(new Choice<>("All statuses", null));
        for (ApplicationStatus status : ApplicationStatus.values()) {
            statusBox.getItems().add(new Choice<>(StatusFormat.applicationStatus(status), status));
        }
        statusBox.getSelectionModel().selectFirst();
        statusBox.setOnAction(event -> load());
        table = new MonitorTable<>("Applications", List.of("Gig", "Freelancer", "Application", "Gig status", "Sent"),
                ApplicationsMonitorTab::toCells, this::load);

        HBox filter = new HBox(UiConstants.SPACING_MEDIUM, new Label("Status:"), statusBox);
        filter.setAlignment(Pos.CENTER_LEFT);
        setMargin(filter, new Insets(0, 0, UiConstants.SPACING_MEDIUM, 0));
        setTop(filter);
        setCenter(table);
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
        Choice<ApplicationStatus> chosen = statusBox.getValue();
        ApplicationStatus status = chosen == null ? null : chosen.value();
        runInBackground(() -> monitoringService.listApplications(adminId, status), table::show);
    }
}
