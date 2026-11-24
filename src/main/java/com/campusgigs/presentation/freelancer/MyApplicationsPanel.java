package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.presentation.common.StatusFormat;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.util.SessionManager;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.util.List;
import java.util.Optional;

/** The freelancer's applications and what became of them (F24); a pending one can be withdrawn (F23). */
public class MyApplicationsPanel extends TablePanel<ApplicationDto> {

    private static final long serialVersionUID = 1L;

    private final transient ApplicationService applicationService;
    private JButton withdrawButton;

    public MyApplicationsPanel(ApplicationService applicationService) {
        super("My Applications", List.of("Gig", "Application", "Gig status", "Sent"), MyApplicationsPanel::toCells);
        this.applicationService = applicationService;
        withdrawButton = addAction("Withdraw", true, this::withdraw);
        addAction("Refresh", false, this::load);
        load();
    }

    /** The cell values of one application, in column order. */
    static Object[] toCells(ApplicationDto application) {
        return new Object[] {
                application.gigTitle(),
                StatusFormat.applicationStatus(application.status()),
                StatusFormat.gigStatus(application.gigStatus()),
                TokenFormat.dateTime(application.submittedAt())
        };
    }

    @Override
    protected void onSelectionChanged(Optional<ApplicationDto> row) {
        if (withdrawButton != null) {
            withdrawButton.setEnabled(row.isPresent() && row.get().status() == ApplicationStatus.PENDING);
        }
    }

    private void load() {
        long freelancerId = currentUserId();
        runInBackground(() -> applicationService.listForFreelancer(freelancerId), this::setRows);
    }

    private void withdraw() {
        selected().ifPresent(application -> {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Withdraw your application for \"" + application.gigTitle() + "\"?",
                    "Withdraw application", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                long freelancerId = currentUserId();
                runInBackground(() -> applicationService.withdraw(freelancerId, application.applicationId()),
                        withdrawn -> load());
            }
        });
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
