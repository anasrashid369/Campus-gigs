package com.campusgigs.presentation.client;

import com.campusgigs.pattern.state.GigAction;
import com.campusgigs.dto.GigDto;
import com.campusgigs.presentation.common.Dialogs;
import com.campusgigs.presentation.common.StatusFormat;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TextPrompt;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.GigService;
import com.campusgigs.service.ProfileService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * The client's own gigs (F14-F18): see their status, change or cancel an open gig, review its
 * applications and hire, then approve the work, ask for a revision or raise a dispute.
 * Which buttons are available comes from {@link GigService#allowedActions}; the services
 * enforce the rules again when a button is used.
 */
public class ClientGigsPanel extends TablePanel<GigDto> {

    private static final String NO_VALUE = "-";

    private final GigService gigService;
    private final ProfileService profileService;
    private final ApplicationService applicationService;
    private final GigLifecycleService lifecycleService;

    private Button editButton;
    private Button cancelButton;
    private Button approveButton;
    private Button revisionButton;
    private Button disputeButton;
    private Button applicationsButton;

    public ClientGigsPanel(GigService gigService, ProfileService profileService,
                           ApplicationService applicationService, GigLifecycleService lifecycleService) {
        super("My Gigs", List.of("Title", "Budget", "Deadline", "Status", "Freelancer"), ClientGigsPanel::toCells);
        this.gigService = gigService;
        this.profileService = profileService;
        this.applicationService = applicationService;
        this.lifecycleService = lifecycleService;
        applicationsButton = addAction("Applications", true, this::showApplications);
        editButton = addAction("Edit", true, this::edit);
        cancelButton = addAction("Cancel gig", true, this::cancel);
        approveButton = addAction("Approve work", true, this::approve);
        revisionButton = addAction("Request revision", true, this::requestRevision);
        disputeButton = addAction("Raise dispute", true, this::raiseDispute);
        addAction("Refresh", false, this::load);
        approveButton.getStyleClass().add("primary");
        cancelButton.getStyleClass().add("danger");
        load();
    }

    /** The cell values of one gig, in column order. */
    static Object[] toCells(GigDto gig) {
        return new Object[] {
                gig.title(),
                TokenFormat.tokens(gig.budget()),
                gig.deadline(),
                StatusFormat.gigStatus(gig.status()),
                gig.assignedFreelancerId() == null ? NO_VALUE : "#" + gig.assignedFreelancerId()
        };
    }

    @Override
    protected void onSelectionChanged(Optional<GigDto> row) {
        if (editButton == null) {
            return;
        }
        Set<GigAction> allowed = row.map(gig -> gigService.allowedActions(gig.status())).orElse(Set.of());
        applicationsButton.setDisable(row.isEmpty());
        editButton.setDisable(!allowed.contains(GigAction.CANCEL));
        cancelButton.setDisable(!allowed.contains(GigAction.CANCEL));
        approveButton.setDisable(!allowed.contains(GigAction.APPROVE));
        revisionButton.setDisable(!allowed.contains(GigAction.REQUEST_REVISION));
        disputeButton.setDisable(!allowed.contains(GigAction.RAISE_DISPUTE));
    }

    private void load() {
        long clientId = currentUserId();
        runInBackground(() -> gigService.listClientGigs(clientId), this::setRows);
    }

    private void showApplications() {
        selected().ifPresent(gig -> {
            Dialogs.modal(this, "Applications", new GigApplicationsPanel(applicationService, gig), 820, 460)
                    .showAndWait();
            load();
        });
    }

    private void edit() {
        selected().ifPresent(gig -> {
            Stage[] window = new Stage[1];
            GigFormPanel form = new GigFormPanel(gigService, profileService, gig, () -> window[0].close());
            window[0] = Dialogs.modal(this, "Change gig", form, 720, 720);
            window[0].showAndWait();
            load();
        });
    }

    private void cancel() {
        selected().ifPresent(gig -> {
            if (Dialogs.confirm(this, "Cancel gig",
                    "Cancel \"" + gig.title() + "\"? Freelancers will no longer see it.")) {
                long clientId = currentUserId();
                runInBackground(() -> gigService.cancelGig(clientId, gig.gigId()), cancelled -> load());
            }
        });
    }

    private void approve() {
        selected().ifPresent(gig -> {
            if (Dialogs.confirm(this, "Approve work", "Approve the work for \"" + gig.title() + "\"? "
                    + TokenFormat.tokens(gig.budget())
                    + " tokens are paid to the freelancer (minus the platform fee).\nThis cannot be undone.")) {
                long clientId = currentUserId();
                runInBackground(() -> lifecycleService.approve(clientId, gig.gigId()), approved -> load());
            }
        });
    }

    private void requestRevision() {
        selected().ifPresent(gig -> {
            if (Dialogs.confirm(this, "Request revision", "Ask the freelancer to revise \"" + gig.title() + "\"?")) {
                long clientId = currentUserId();
                runInBackground(() -> lifecycleService.requestRevision(clientId, gig.gigId()), revised -> load());
            }
        });
    }

    private void raiseDispute() {
        selected().ifPresent(gig -> TextPrompt.ask(this, "Raise a dispute for \"" + gig.title() + "\"",
                "Describe the problem for the administrator (1 - " + GigLifecycleService.MAX_DISPUTE_REASON_LENGTH
                        + " characters)", 1, GigLifecycleService.MAX_DISPUTE_REASON_LENGTH).ifPresent(reason -> {
            long clientId = currentUserId();
            runInBackground(() -> lifecycleService.raiseDispute(clientId, gig.gigId(), reason), disputed -> load());
        }));
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
