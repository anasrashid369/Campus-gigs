package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.GigDto;
import com.campusgigs.pattern.state.GigAction;
import com.campusgigs.presentation.common.StatusFormat;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TextPrompt;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.GigService;
import com.campusgigs.util.SessionManager;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The gigs the freelancer was hired for (F25): deadlines, overdue marks, delivering the work and
 * raising a dispute. A button is available when {@link GigService#allowedActions} allows the move;
 * {@link GigLifecycleService} checks the rules again when it is used.
 */
public class FreelancerGigsPanel extends TablePanel<GigDto> {

    private static final long serialVersionUID = 1L;
    private static final String OVERDUE = "Overdue";

    private final transient GigService gigService;
    private final transient GigLifecycleService lifecycleService;
    private JButton submitButton;
    private JButton disputeButton;

    public FreelancerGigsPanel(GigService gigService, GigLifecycleService lifecycleService) {
        super("My Gigs", List.of("Title", "Budget", "Deadline", "Status", ""), gig -> toCells(gig, LocalDate.now()));
        this.gigService = gigService;
        this.lifecycleService = lifecycleService;
        submitButton = addAction("Submit work", true, this::submitWork);
        disputeButton = addAction("Raise dispute", true, this::raiseDispute);
        addAction("Refresh", false, this::load);
        load();
    }

    /** The cell values of one gig, in column order; the last column marks late work. */
    static Object[] toCells(GigDto gig, LocalDate today) {
        return new Object[] {
                gig.title(),
                TokenFormat.tokens(gig.budget()),
                gig.deadline(),
                StatusFormat.gigStatus(gig.status()),
                StatusFormat.isOverdue(gig, today) ? OVERDUE : ""
        };
    }

    @Override
    protected void onSelectionChanged(Optional<GigDto> row) {
        if (submitButton == null) {
            return;
        }
        submitButton.setEnabled(row.isPresent() && allows(row.get(), GigAction.SUBMIT));
        disputeButton.setEnabled(row.isPresent() && allows(row.get(), GigAction.RAISE_DISPUTE));
    }

    private boolean allows(GigDto gig, GigAction action) {
        return gigService.allowedActions(gig.status()).contains(action);
    }

    private void load() {
        long freelancerId = currentUserId();
        runInBackground(() -> gigService.listFreelancerGigs(freelancerId), this::setRows);
    }

    private void submitWork() {
        selected().ifPresent(gig -> {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Hand in your work for \"" + gig.title() + "\"? The client will review it.",
                    "Submit work", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                long freelancerId = currentUserId();
                runInBackground(() -> lifecycleService.submitWork(freelancerId, gig.gigId()), submitted -> load());
            }
        });
    }

    private void raiseDispute() {
        selected().ifPresent(gig -> TextPrompt.ask(this, "Raise a dispute for \"" + gig.title() + "\"",
                "Describe the problem for the administrator (1 - " + GigLifecycleService.MAX_DISPUTE_REASON_LENGTH
                        + " characters)", 1, GigLifecycleService.MAX_DISPUTE_REASON_LENGTH).ifPresent(reason -> {
            long freelancerId = currentUserId();
            runInBackground(() -> lifecycleService.raiseDispute(freelancerId, gig.gigId(), reason), disputed -> load());
        }));
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
