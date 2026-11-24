package com.campusgigs.presentation.client;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.presentation.common.StatusFormat;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TextFormat;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.util.SessionManager;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.util.List;
import java.util.Optional;

/**
 * The applications for one gig (F16) and the Hire button (F17). Hiring is done by
 * {@link ApplicationService#hire}, which also funds the gig; this panel only asks the client to confirm.
 */
public class GigApplicationsPanel extends TablePanel<ApplicationDto> {

    private static final long serialVersionUID = 1L;
    private static final int PROPOSAL_CHARS = 60;

    private final transient ApplicationService applicationService;
    private final transient GigDto gig;
    private JButton hireButton;

    public GigApplicationsPanel(ApplicationService applicationService, GigDto gig) {
        super("Applications for \"" + gig.title() + "\"", List.of("Freelancer", "Status", "Sent", "Proposal"),
                GigApplicationsPanel::toCells);
        this.applicationService = applicationService;
        this.gig = gig;
        addAction("View proposal", true, this::viewProposal);
        hireButton = addAction("Hire", true, this::hire);
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
            hireButton.setEnabled(row.isPresent() && row.get().status() == ApplicationStatus.PENDING
                    && row.get().gigStatus() == GigStatus.OPEN);
        }
    }

    private void load() {
        long clientId = currentUserId();
        runInBackground(() -> applicationService.listForGig(clientId, gig.gigId()), this::setRows);
    }

    private void viewProposal() {
        selected().ifPresent(application -> {
            JTextArea text = new JTextArea(application.proposalMessage(), 8, 40);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(this, new JScrollPane(text),
                    "Proposal of " + application.freelancerName(), JOptionPane.PLAIN_MESSAGE);
        });
    }

    private void hire() {
        selected().ifPresent(application -> {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Hire " + application.freelancerName() + " for \"" + gig.title() + "\"?\n"
                            + TokenFormat.tokens(gig.budget()) + " tokens will be moved into escrow now.",
                    "Hire freelancer", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                long clientId = currentUserId();
                runInBackground(() -> applicationService.hire(clientId, gig.gigId(), application.applicationId()),
                        hired -> {
                            JOptionPane.showMessageDialog(this, application.freelancerName() + " is hired.",
                                    "Freelancer hired", JOptionPane.INFORMATION_MESSAGE);
                            load();
                        });
            }
        });
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
