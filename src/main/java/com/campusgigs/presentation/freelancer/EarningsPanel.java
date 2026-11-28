package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.EarningsDto;
import com.campusgigs.dto.TransactionDto;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.StatCard;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.presentation.common.TransactionHistoryPanel;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.EarningsService;
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
 * Freelancer earnings screen (F26, F27): available tokens, lifetime earnings after fees,
 * tokens still waiting for approval, a simulated withdrawal form and the transaction history.
 * The rules (freelancers only, amount limits, available balance) live in the services.
 */
public class EarningsPanel extends BasePanel {

    private static final String AMOUNT_HINT = "Enter a whole number of tokens";

    private final EarningsService earningsService;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final StatCard availableCard = new StatCard("Available to withdraw");
    private final StatCard earnedCard = new StatCard("Total earned (after fees)");
    private final StatCard pendingCard = new StatCard("Pending approval");
    private final StatCard withdrawnCard = new StatCard("Total withdrawn");
    private final TextField amountField = new TextField();
    private final Button withdrawButton = new Button("Withdraw");
    private final Label messageLabel = FormLayout.messageLabel();
    private final TransactionHistoryPanel historyPanel = new TransactionHistoryPanel();

    public EarningsPanel(EarningsService earningsService, WalletService walletService,
                         TransactionService transactionService) {
        this.earningsService = earningsService;
        this.walletService = walletService;
        this.transactionService = transactionService;

        VBox top = new VBox(UiConstants.SPACING_MEDIUM,
                StatCard.row(availableCard, earnedCard, pendingCard, withdrawnCard),
                createWithdrawForm(), messageLabel);
        setMargin(top, new Insets(0, 0, UiConstants.SPACING_LARGE, 0));
        setTop(top);
        setCenter(historyPanel);
        reload();
    }

    private HBox createWithdrawForm() {
        Label caption = new Label("Withdraw tokens (simulated, at least " + WalletService.MIN_WITHDRAWAL + "):");
        caption.setLabelFor(amountField);
        amountField.setPromptText("Amount");
        amountField.setPrefColumnCount(8);
        amountField.setOnAction(event -> submitWithdrawal(amountField.getText()));
        withdrawButton.getStyleClass().add("primary");
        withdrawButton.setOnAction(event -> submitWithdrawal(amountField.getText()));
        HBox row = new HBox(UiConstants.SPACING_MEDIUM, caption, amountField, withdrawButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(UiConstants.SPACING_MEDIUM, 0, 0, 0));
        return row;
    }

    private void reload() {
        long userId = currentUserId();
        runInBackground(() -> snapshot(userId), this::show);
    }

    /** Validates the typed amount, then withdraws and refreshes the screen. */
    void submitWithdrawal(String text) {
        long amount;
        try {
            amount = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            FormLayout.showError(messageLabel, AMOUNT_HINT);
            return;
        }
        long userId = currentUserId();
        withdrawButton.setDisable(true);
        runInBackground(() -> {
            walletService.withdraw(userId, amount);
            return snapshot(userId);
        }, snapshot -> {
            show(snapshot);
            amountField.clear();
            withdrawButton.setDisable(false);
            FormLayout.showSuccess(messageLabel, TokenFormat.tokens(amount) + " tokens withdrawn");
        }, error -> {
            withdrawButton.setDisable(false);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }

    private Snapshot snapshot(long userId) {
        return new Snapshot(earningsService.getEarnings(userId), transactionService.getHistory(userId));
    }

    private void show(Snapshot snapshot) {
        EarningsDto earnings = snapshot.earnings();
        availableCard.setTokens(earnings.availableBalance());
        earnedCard.setTokens(earnings.totalEarned());
        pendingCard.setTokens(earnings.pendingPayout());
        withdrawnCard.setTokens(earnings.totalWithdrawn());
        historyPanel.showHistory(snapshot.history());
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }

    /** Earnings and ledger loaded together so the screen updates in one step. */
    private record Snapshot(EarningsDto earnings, List<TransactionDto> history) {
    }
}
