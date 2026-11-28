package com.campusgigs.presentation.admin;

import com.campusgigs.dto.DisputeDto;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;
import com.campusgigs.presentation.common.Dialogs;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TextFormat;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.service.DisputeService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Button;

/**
 * Admin screen for disputes (F10, F11): review open or resolved disputes, read the reason and
 * decide an open one by paying the freelancer (minus the platform fee) or refunding the client.
 * Who may decide, and every money rule, is enforced by {@link DisputeService}.
 */
public class DisputesPanel extends TablePanel<DisputeDto> {

    private static final int MAX_REASON_CHARS = 60;
    private static final String NO_VALUE = "-";

    private final DisputeService disputeService;
    private DisputeStatus shown = DisputeStatus.OPEN;
    private Button releaseButton;
    private Button refundButton;

    public DisputesPanel(DisputeService disputeService) {
        super("Disputes", List.of("Dispute", "Gig", "Budget", "Raised by", "Status", "Decision", "Reason"),
                DisputesPanel::toCells);
        this.disputeService = disputeService;
        addAction("Open disputes", false, () -> show(DisputeStatus.OPEN));
        addAction("Resolved disputes", false, () -> show(DisputeStatus.RESOLVED));
        addAction("View reason", true, this::viewReason);
        releaseButton = addAction("Release to freelancer", true, () -> decide(DisputeDecision.RELEASE));
        refundButton = addAction("Refund client", true, () -> decide(DisputeDecision.REFUND));
        addAction("Refresh", false, this::load);
        releaseButton.getStyleClass().add("primary");
        load();
    }

    /** The cell values of one dispute, in column order. */
    static Object[] toCells(DisputeDto dispute) {
        return new Object[] {
                "#" + dispute.disputeId(),
                dispute.gigTitle(),
                TokenFormat.tokens(dispute.budget()),
                dispute.raisedBy() == dispute.clientId() ? "Client" : "Freelancer",
                dispute.status(),
                dispute.decision() == null ? NO_VALUE : dispute.decision(),
                shorten(dispute.reason())
        };
    }

    /** Cuts a long reason to one table line, adding an ellipsis. */
    static String shorten(String reason) {
        return TextFormat.oneLine(reason, MAX_REASON_CHARS);
    }

    @Override
    protected void onSelectionChanged(Optional<DisputeDto> row) {
        boolean open = row.isPresent() && row.get().status() == DisputeStatus.OPEN;
        if (releaseButton != null) {
            releaseButton.setDisable(!open);
            refundButton.setDisable(!open);
        }
    }

    private void show(DisputeStatus status) {
        shown = status;
        load();
    }

    private void load() {
        long adminId = adminId();
        DisputeStatus status = shown;
        runInBackground(() -> disputeService.listDisputes(adminId, status), this::setRows);
    }

    private void viewReason() {
        selected().ifPresent(dispute -> Dialogs.showText(this,
                "Dispute #" + dispute.disputeId() + " - " + dispute.gigTitle(), dispute.reason()));
    }

    private void decide(DisputeDecision decision) {
        selected().ifPresent(dispute -> {
            String question = decision == DisputeDecision.RELEASE
                    ? "Pay the freelancer for \"" + dispute.gigTitle() + "\" (" + TokenFormat.tokens(dispute.budget())
                            + " tokens, minus the platform fee)?"
                    : "Refund " + TokenFormat.tokens(dispute.budget()) + " tokens to the client for \""
                            + dispute.gigTitle() + "\"?";
            if (Dialogs.confirm(this, "Resolve dispute #" + dispute.disputeId(),
                    question + "\nThis cannot be undone.")) {
                resolve(dispute.disputeId(), decision);
            }
        });
    }

    private void resolve(long disputeId, DisputeDecision decision) {
        long adminId = adminId();
        runInBackground(() -> disputeService.resolve(adminId, disputeId, decision), resolved -> load());
    }

    private static long adminId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
