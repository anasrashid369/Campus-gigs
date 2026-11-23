package com.campusgigs.presentation.client;

import com.campusgigs.dto.GigDto;
import com.campusgigs.pattern.state.GigAction;
import com.campusgigs.presentation.common.StatusFormat;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TextPrompt;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.GigService;
import com.campusgigs.service.ProfileService;
import com.campusgigs.util.SessionManager;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The client's own gigs (F14-F18): see their status, change or cancel an open gig, review its
 * applications and hire, then approve the work, ask for a revision or raise a dispute.
 * Which buttons are available comes from {@link GigService#allowedActions}; the services
 * enforce the rules again when a button is used.
 */
public class ClientGigsPanel extends TablePanel<GigDto> {

    private static final long serialVersionUID = 1L;
    private static final String NO_VALUE = "-";

    private final transient GigService gigService;
    private final transient ProfileService profileService;
    private final transient ApplicationService applicationService;
    private final transient GigLifecycleService lifecycleService;
    private JButton editButton;
    private JButton cancelButton;
    private JButton approveButton;
    private JButton revisionButton;
    private JButton disputeButton;
    private JButton applicationsButton;

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
        applicationsButton.setEnabled(row.isPresent());
        editButton.setEnabled(allowed.contains(GigAction.CANCEL));
        cancelButton.setEnabled(allowed.contains(GigAction.CANCEL));
        approveButton.setEnabled(allowed.contains(GigAction.APPROVE));
        revisionButton.setEnabled(allowed.contains(GigAction.REQUEST_REVISION));
        disputeButton.setEnabled(allowed.contains(GigAction.RAISE_DISPUTE));
    }

    private void load() {
        long clientId = currentUserId();
        runInBackground(() -> gigService.listClientGigs(clientId), this::setRows);
    }

    private void showApplications() {
        selected().ifPresent(gig -> {
            JDialog dialog = newDialog("Applications");
            dialog.setContentPane(new GigApplicationsPanel(applicationService, gig));
            dialog.setSize(760, 420);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            load();
        });
    }

    private void edit() {
        selected().ifPresent(gig -> {
            JDialog dialog = newDialog("Change gig");
            dialog.setContentPane(new GigFormPanel(gigService, profileService, gig, dialog::dispose));
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            load();
        });
    }

    private JDialog newDialog(String title) {
        return new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
    }

    private void cancel() {
        selected().ifPresent(gig -> {
            if (confirm("Cancel \"" + gig.title() + "\"? Freelancers will no longer see it.", "Cancel gig")) {
                long clientId = currentUserId();
                runInBackground(() -> gigService.cancelGig(clientId, gig.gigId()), cancelled -> load());
            }
        });
    }

    private void approve() {
        selected().ifPresent(gig -> {
            if (confirm("Approve the work for \"" + gig.title() + "\"? " + TokenFormat.tokens(gig.budget())
                    + " tokens are paid to the freelancer (minus the platform fee).\nThis cannot be undone.",
                    "Approve work")) {
                long clientId = currentUserId();
                runInBackground(() -> lifecycleService.approve(clientId, gig.gigId()), approved -> load());
            }
        });
    }

    private void requestRevision() {
        selected().ifPresent(gig -> {
            if (confirm("Ask the freelancer to revise \"" + gig.title() + "\"?", "Request revision")) {
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

    private boolean confirm(String question, String title) {
        return JOptionPane.showConfirmDialog(this, question, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
