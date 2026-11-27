package com.campusgigs.presentation.common;

import com.campusgigs.dto.TransactionDto;
import com.campusgigs.dto.WalletDto;
import com.campusgigs.service.TransactionService;
import com.campusgigs.service.WalletService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Client wallet screen (F5, F6, F20): the available and escrowed balances, a top-up form
 * and the transaction history. All rules (amount range, client-only top-up, the simulated
 * payment) live in {@link WalletService}; this screen only collects the amount, calls the
 * service and shows the result.
 */
public class WalletPanel extends BasePanel {

    private static final String AMOUNT_HINT = "Enter a whole number of tokens";

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final StatCard availableCard = new StatCard("Available tokens");
    private final StatCard escrowCard = new StatCard("Held in escrow");
    private final TextField amountField = new TextField();
    private final Button topUpButton = new Button("Top up");
    private final Label messageLabel = FormLayout.messageLabel();
    private final TransactionHistoryPanel historyPanel = new TransactionHistoryPanel();

    public WalletPanel(WalletService walletService, TransactionService transactionService) {
        this.walletService = walletService;
        this.transactionService = transactionService;

        VBox top = new VBox(UiConstants.SPACING_MEDIUM, StatCard.row(availableCard, escrowCard),
                createTopUpForm(), messageLabel);
        setMargin(top, new Insets(0, 0, UiConstants.SPACING_LARGE, 0));
        setTop(top);
        setCenter(historyPanel);
        reload();
    }

    private HBox createTopUpForm() {
        Label caption = new Label("Top up (" + WalletService.MIN_TOP_UP + " - "
                + TokenFormat.tokens(WalletService.MAX_TOP_UP) + " tokens):");
        caption.setLabelFor(amountField);
        amountField.setPromptText("Amount");
        amountField.setPrefColumnCount(8);
        amountField.setOnAction(event -> submitTopUp(amountField.getText()));
        topUpButton.getStyleClass().add("primary");
        topUpButton.setOnAction(event -> submitTopUp(amountField.getText()));
        HBox row = new HBox(UiConstants.SPACING_MEDIUM, caption, amountField, topUpButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(UiConstants.SPACING_MEDIUM, 0, 0, 0));
        return row;
    }

    /** Loads the wallet and its history in the background. */
    private void reload() {
        long userId = currentUserId();
        runInBackground(() -> snapshot(userId), this::show);
    }

    /** Validates the typed amount, then tops up and refreshes the screen. */
    void submitTopUp(String text) {
        long amount;
        try {
            amount = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            FormLayout.showError(messageLabel, AMOUNT_HINT);
            return;
        }
        long userId = currentUserId();
        topUpButton.setDisable(true);
        runInBackground(() -> {
            walletService.topUp(userId, amount);
            return snapshot(userId);
        }, snapshot -> {
            show(snapshot);
            amountField.clear();
            topUpButton.setDisable(false);
            FormLayout.showSuccess(messageLabel, TokenFormat.tokens(amount) + " tokens added");
        }, error -> {
            topUpButton.setDisable(false);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }

    private Snapshot snapshot(long userId) {
        return new Snapshot(walletService.getWallet(userId), transactionService.getHistory(userId));
    }

    private void show(Snapshot snapshot) {
        availableCard.setTokens(snapshot.wallet().availableBalance());
        escrowCard.setTokens(snapshot.wallet().escrowBalance());
        historyPanel.showHistory(snapshot.history());
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }

    /** Wallet balances and ledger loaded together so the screen updates in one step. */
    private record Snapshot(WalletDto wallet, List<TransactionDto> history) {
    }
}
