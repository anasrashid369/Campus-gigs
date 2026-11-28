package com.campusgigs.presentation.client;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.RatingSummaryDto;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.presentation.common.Dialogs;
import com.campusgigs.presentation.common.RatingFormat;
import com.campusgigs.presentation.common.StatusFormat;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TextFormat;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.ReviewService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.scene.control.Button;

/**
 * The applications for one gig (F16) with each freelancer's rating (F19), and the Hire button (F17).
 * Hiring is done by {@link ApplicationService#hire}, which also funds the gig; this screen only asks
 * the client to confirm.
 */
public class GigApplicationsPanel extends TablePanel<GigApplicationsPanel.Row> {

    private static final int PROPOSAL_CHARS = 60;

    /** One application with the rating of the freelancer who sent it. */
    record Row(ApplicationDto application, RatingSummaryDto rating) {
    }

    private final ApplicationService applicationService;
    private final ReviewService reviewService;
    private final GigDto gig;
    private Button hireButton;

    public GigApplicationsPanel(ApplicationService applicationService, ReviewService reviewService, GigDto gig) {
        super("Applications for \"" + gig.title() + "\"",
                List.of("Freelancer", "Rating", "Status", "Sent", "Proposal"), GigApplicationsPanel::toCells);
        this.applicationService = applicationService;
        this.reviewService = reviewService;
        this.gig = gig;
        addAction("View proposal", true, this::viewProposal);
        hireButton = addAction("Hire", true, this::hire);
        hireButton.getStyleClass().add("primary");
        addAction("Refresh", false, this::load);
        load();
    }

    /** The cell values of one row, in column order. */
    static Object[] toCells(Row row) {
        ApplicationDto application = row.application();
        return new Object[] {
                application.freelancerName(),
                RatingFormat.summary(row.rating()),
                StatusFormat.applicationStatus(application.status()),
                TokenFormat.dateTime(application.submittedAt()),
                TextFormat.oneLine(application.proposalMessage(), PROPOSAL_CHARS)
        };
    }

    @Override
    protected void onSelectionChanged(Optional<Row> row) {
        if (hireButton != null) {
            hireButton.setDisable(!(row.isPresent() && row.get().application().status() == ApplicationStatus.PENDING
                    && row.get().application().gigStatus() == GigStatus.OPEN));
        }
    }

    private void load() {
        long clientId = currentUserId();
        runInBackground(() -> {
            List<ApplicationDto> applications = applicationService.listForGig(clientId, gig.gigId());
            Map<Long, RatingSummaryDto> ratings = reviewService.getRatingSummaries(
                    applications.stream().map(ApplicationDto::freelancerId).toList());
            return applications.stream()
                    .map(application -> new Row(application, ratings.get(application.freelancerId())))
                    .toList();
        }, this::setRows);
    }

    private void viewProposal() {
        selected().map(Row::application).ifPresent(application -> Dialogs.showText(this,
                "Proposal of " + application.freelancerName(), application.proposalMessage()));
    }

    private void hire() {
        selected().map(Row::application).ifPresent(application -> {
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
