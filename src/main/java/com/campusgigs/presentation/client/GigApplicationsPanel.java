package com.campusgigs.presentation.client;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.presentation.common.Dialogs;
import com.campusgigs.presentation.common.StatusFormat;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TextFormat;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Button;

/**
 * The applications for one gig (F16) and the Hire button (F17). Hiring is done by
 * {@link ApplicationService#hire}, which also funds the gig; this screen only asks the client to confirm.
 */
public class GigApplicationsPanel extends TablePanel<ApplicationDto> {

    private static final int PROPOSAL_CHARS = 60;

    private final ApplicationService applicationService;
    private final GigDto gig;
    private Button hireButton;

    public GigApplicationsPanel(ApplicationService applicationService, GigDto gig) {
        super("Applications for \"" + gig.title() + "\"", List.of("Freelancer", "Status", "Sent", "Proposal"),
                GigApplicationsPanel::toCells);
        this.applicationService = applicationService;
        this.gig = gig;
        addAction("View proposal", true, this::viewProposal);
        hireButton = addAction("Hire", true, this::hire);
        hireButton.getStyleClass().add("primary");
        addAction("Refresh", false, this::load);
        load();
    }

    /** The cell values of one application, in column order. */
    static Object[] toCells(ApplicationDto application) {
        return new Object[] {
                application.freelancerName(),
                StatusFormat.applicationStatus(application.status()),
                TokenFormat.dateTime(application.submittedAt()),
                TextFormat.oneLine(application.proposalMessage(), PROPOSAL_CHARS)
        };
    }

    @Override
    protected void onSelectionChanged(Optional<ApplicationDto> row) {
        if (hireButton != null) {
            hireButton.setDisable(!(row.isPresent() && row.get().status() == ApplicationStatus.PENDING
                    && row.get().gigStatus() == GigStatus.OPEN));
        }
    }

    private void load() {
        long clientId = currentUserId();
        runInBackground(() -> applicationService.listForGig(clientId, gig.gigId()), this::setRows);
    }

    private void viewProposal() {
        selected().ifPresent(application -> Dialogs.showText(this,
                "Proposal of " + application.freelancerName(), application.proposalMessage()));
    }

    private void hire() {
        selected().ifPresent(application -> {
            if (Dialogs.confirm(this, "Hire freelancer",
                    "Hire " + application.freelancerName() + " for \"" + gig.title() + "\"?\n"
                            + TokenFormat.tokens(gig.budget()) + " tokens will be moved into escrow now.")) {
                long clientId = currentUserId();
                runInBackground(() -> applicationService.hire(clientId, gig.gigId(), application.applicationId()),
                        hired -> {
                            Dialogs.info(this, "Freelancer hired", application.freelancerName() + " is hired.");
                            load();
                        });
            }
        });
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
