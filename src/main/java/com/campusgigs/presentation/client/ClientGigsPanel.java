package com.campusgigs.presentation.client;

import com.campusgigs.dto.GigDto;
import com.campusgigs.pattern.state.GigAction;
import com.campusgigs.presentation.common.StatusFormat;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TokenFormat;
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

/**
 * The client's own gigs (F14, F15): see their status, change an open gig or cancel it.
 * Whether a button is available comes from {@link GigService#allowedActions}, and the
 * service enforces the rules again when the button is used.
 */
public class ClientGigsPanel extends TablePanel<GigDto> {

    private static final long serialVersionUID = 1L;
    private static final String NO_VALUE = "-";

    private final transient GigService gigService;
    private final transient ProfileService profileService;
    private JButton editButton;
    private JButton cancelButton;

    public ClientGigsPanel(GigService gigService, ProfileService profileService) {
        super("My Gigs", List.of("Title", "Budget", "Deadline", "Status", "Freelancer"), ClientGigsPanel::toCells);
        this.gigService = gigService;
        this.profileService = profileService;
        editButton = addAction("Edit", true, this::edit);
        cancelButton = addAction("Cancel gig", true, this::cancel);
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
        boolean open = row.isPresent() && gigService.allowedActions(row.get().status()).contains(GigAction.CANCEL);
        editButton.setEnabled(open);
        cancelButton.setEnabled(open);
    }

    private void load() {
        long clientId = currentUserId();
        runInBackground(() -> gigService.listClientGigs(clientId), this::setRows);
    }

    private void edit() {
        selected().ifPresent(gig -> {
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Change gig",
                    Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setContentPane(new GigFormPanel(gigService, profileService, gig, dialog::dispose));
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            load();
        });
    }

    private void cancel() {
        selected().ifPresent(gig -> {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Cancel \"" + gig.title() + "\"? Freelancers will no longer see it.",
                    "Cancel gig", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                long clientId = currentUserId();
                runInBackground(() -> gigService.cancelGig(clientId, gig.gigId()), cancelled -> load());
            }
        });
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
